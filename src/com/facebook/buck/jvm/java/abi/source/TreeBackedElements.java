/*
 * Copyright 2016-present Facebook, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.facebook.buck.jvm.java.abi.source;

import com.facebook.buck.util.liteinfersupport.Nullable;
import com.facebook.buck.util.liteinfersupport.Preconditions;
import com.sun.source.util.Trees;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;

/**
 * An implementation of {@link Elements} using just the AST of a single module, without its
 * dependencies. Of necessity, such an implementation will need to make assumptions about the
 * meanings of some names, and thus must be used with care. See documentation for individual
 * methods and {@link com.facebook.buck.jvm.java.abi.source} for more information.
 */
class TreeBackedElements implements Elements {
  private final Elements javacElements;
  private final Trees javacTrees;
  private final Map<Element, TreeBackedElement> treeBackedElements = new HashMap<>();
  private final Map<Name, TypeElement> knownTypes = new HashMap<>();
  private final Map<Name, TreeBackedPackageElement> knownPackages = new HashMap<>();

  @Nullable
  private TreeBackedElementResolver resolver;

  public TreeBackedElements(Elements javacElements, Trees javacTrees) {
    this.javacElements = javacElements;
    this.javacTrees = javacTrees;
  }

  /* package */ void setResolver(TreeBackedElementResolver resolver) {
    this.resolver = resolver;
  }

  /* package */ void clear() {
    treeBackedElements.clear();
    knownTypes.clear();
    knownPackages.clear();
  }

  public TreeBackedElement enterElement(Element underlyingElement) {
    TreeBackedElement result = treeBackedElements.get(underlyingElement);
    if (result != null) {
      return result;
    }

    ElementKind kind = underlyingElement.getKind();
    switch (kind) {
      case PACKAGE:
        result = newTreeBackedPackage((PackageElement) underlyingElement);
        break;
      case ANNOTATION_TYPE:
      case CLASS:
      case ENUM:
      case INTERFACE:
        result = newTreeBackedType((TypeElement) underlyingElement);
        break;
      case TYPE_PARAMETER:
        result = newTreeBackedTypeParameter((TypeParameterElement) underlyingElement);
        break;
      case ENUM_CONSTANT:
      case FIELD:
      case PARAMETER:
        result = newTreeBackedVariable((VariableElement) underlyingElement);
        break;
      case CONSTRUCTOR:
      case METHOD:
        result = newTreeBackedExecutable((ExecutableElement) underlyingElement);
        break;
      // $CASES-OMITTED$
      default:
        throw new UnsupportedOperationException(String.format("Element kind %s NYI", kind));
    }

    treeBackedElements.put(underlyingElement, result);

    return result;
  }

  private TreeBackedPackageElement newTreeBackedPackage(PackageElement underlyingPackage) {
    TreeBackedPackageElement treeBackedPackage =
        new TreeBackedPackageElement(
            underlyingPackage,
            Preconditions.checkNotNull(resolver));

    knownPackages.put(treeBackedPackage.getQualifiedName(), treeBackedPackage);

    return treeBackedPackage;
  }

  private TreeBackedTypeElement newTreeBackedType(TypeElement underlyingType) {
    TreeBackedTypeElement treeBackedType =
        new TreeBackedTypeElement(
            underlyingType,
            enterElement(underlyingType.getEnclosingElement()),
            Preconditions.checkNotNull(javacTrees.getPath(underlyingType)),
            Preconditions.checkNotNull(resolver)
        );

    knownTypes.put(treeBackedType.getQualifiedName(), treeBackedType);

    return treeBackedType;
  }

  private TreeBackedTypeParameterElement newTreeBackedTypeParameter(
      TypeParameterElement underlyingTypeParameter) {
    TreeBackedParameterizable enclosingElement =
        (TreeBackedParameterizable) enterElement(underlyingTypeParameter.getEnclosingElement());
    return new TreeBackedTypeParameterElement(
        underlyingTypeParameter,
        Preconditions.checkNotNull(javacTrees.getPath(underlyingTypeParameter)),
        enclosingElement,
        Preconditions.checkNotNull(resolver)
    );
  }

  private TreeBackedExecutableElement newTreeBackedExecutable(
      ExecutableElement underlyingExecutable) {
    return new TreeBackedExecutableElement(
        underlyingExecutable,
        enterElement(underlyingExecutable.getEnclosingElement()),
        javacTrees.getPath(underlyingExecutable),
        Preconditions.checkNotNull(resolver));
  }

  private TreeBackedVariableElement newTreeBackedVariable(VariableElement underlyingVariable) {
    return new TreeBackedVariableElement(
        underlyingVariable,
        enterElement(underlyingVariable.getEnclosingElement()),
        javacTrees.getPath(underlyingVariable),
        Preconditions.checkNotNull(resolver));
  }

  @Nullable
  /* package */ TypeElement getCanonicalElement(@Nullable TypeElement element) {
    return (TypeElement) getCanonicalElement((Element) element);
  }

  @Nullable
  /* package */ ExecutableElement getCanonicalElement(@Nullable ExecutableElement element) {
    return (ExecutableElement) getCanonicalElement((Element) element);
  }

  /**
   * Given a javac Element, gets the element that should be used by callers to refer to it. For
   * elements that have ASTs, that will be a TreeBackedElement; otherwise the javac Element itself.
   */
  @Nullable
  /* package */ Element getCanonicalElement(@Nullable Element element) {
    Element result = treeBackedElements.get(element);
    if (result == null) {
      result = element;
    }

    return result;
  }

  /* package */ TypeElement getJavacElement(TypeElement element) {
    return (TypeElement) getJavacElement((Element) element);
  }

  /* package */ Element getJavacElement(Element element) {
    if (element instanceof TreeBackedElement) {
      TreeBackedElement treeBackedElement = (TreeBackedElement) element;
      return treeBackedElement.getUnderlyingElement();
    }

    return element;
  }

  /**
   * Gets the package element with the given name. If a package with the given name is referenced
   * in the code or exists in the classpath, returns the corresponding element. Otherwise returns
   * null.
   */
  @Override
  @Nullable
  public TreeBackedPackageElement getPackageElement(CharSequence qualifiedNameString) {
    Name qualifiedName = getName(qualifiedNameString);

    if (!knownPackages.containsKey(qualifiedName)) {
      PackageElement javacPackageElement = javacElements.getPackageElement(qualifiedName);
      if (javacPackageElement != null) {
        enterElement(javacPackageElement);
      }
    }

    return knownPackages.get(qualifiedName);
  }

  /**
   * Gets the type element with the given name. If a class with the given name is referenced in
   * the code or exists in the classpath, returns the corresponding element. Otherwise returns
   * null.
   */
  @Override
  @Nullable
  public TypeElement getTypeElement(CharSequence fullyQualifiedCharSequence) {
    Name fullyQualifiedName = getName(fullyQualifiedCharSequence);
    if (!knownTypes.containsKey(fullyQualifiedName)) {
      // If none of the types for which we have parse trees matches this fully-qualified name,
      // ask javac. javac will check the classpath, which will pick up built-ins (like java.lang)
      // and any types from dependency targets that are already compiled and on the classpath.
      // Because our tree-backed elements and javac's elements are sharing a name table, we
      // should be able to mix implementations without causing too much trouble.
      TypeElement javacElement = javacElements.getTypeElement(fullyQualifiedName);
      if (javacElement != null) {
        knownTypes.put(fullyQualifiedName, javacElement);
      }
    }

    return knownTypes.get(fullyQualifiedName);
  }

  @Override
  public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValuesWithDefaults(
      AnnotationMirror a) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getDocComment(Element e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isDeprecated(Element e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Name getBinaryName(TypeElement type) {
    return javacElements.getBinaryName(getJavacElement(type));
  }

  @Override
  public PackageElement getPackageOf(Element type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<? extends Element> getAllMembers(TypeElement type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public List<? extends AnnotationMirror> getAllAnnotationMirrors(Element e) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean hides(Element hider, Element hidden) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean overrides(
      ExecutableElement overrider, ExecutableElement overridden, TypeElement type) {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getConstantExpression(Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void printElements(Writer w, Element... elements) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Name getName(CharSequence cs) {
    return javacElements.getName(cs);
  }

  @Override
  public boolean isFunctionalInterface(TypeElement type) {
    throw new UnsupportedOperationException();
  }
}
