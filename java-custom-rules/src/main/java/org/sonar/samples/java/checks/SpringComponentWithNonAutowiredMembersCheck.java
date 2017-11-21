/*
 * SonarQube Java Custom Rules Example
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.samples.java.checks;

import java.util.List;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.SymbolMetadata;
import org.sonar.plugins.java.api.semantic.SymbolMetadata.AnnotationValue;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.LiteralTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.plugins.java.api.tree.VariableTree;

import com.google.common.collect.ImmutableList;

/**
 * 使用原有SpringComponentWithNonAutowiredMembersCheck時，Scope=Prototype型態的也會被拉出來檢查，但這種的應不需要檢查，所以做略過 
 * @author kenneth
 *
 */
@Rule(key = "S3749-TWIBM")
public class SpringComponentWithNonAutowiredMembersCheck extends IssuableSubscriptionVisitor {
  private static final String SCOPE_ANNOTATION_FQN = "org.springframework.context.annotation.Scope";

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    ClassTree clazzTree = (ClassTree) tree;
    SymbolMetadata clazzMeta = clazzTree.symbol().metadata();

    if (isSpringComponent(clazzMeta) && !isScopePrototype(clazzMeta)) {
      clazzTree.members().stream().filter(v -> v.is(Kind.VARIABLE))
        .map(m -> (VariableTree) m)
        .filter(v -> !v.symbol().isStatic())
        .filter(v -> !isSpringInjectionAnnotated(v.symbol().metadata()))
        .forEach(v -> reportIssue(v.simpleName(), "Annotate this member with \"@Autowired\", \"@Resource\", \"@Inject\", or \"@Value\", or remove it."));
    }
  }

  private static boolean isSpringInjectionAnnotated(SymbolMetadata metadata) {
    return metadata.isAnnotatedWith("org.springframework.beans.factory.annotation.Autowired")
      || metadata.isAnnotatedWith("javax.inject.Inject")
      || metadata.isAnnotatedWith("javax.annotation.Resource")
      || metadata.isAnnotatedWith("org.springframework.beans.factory.annotation.Value");
  }

  private static boolean isSpringComponent(SymbolMetadata clazzMeta) {
	return clazzMeta.isAnnotatedWith("org.springframework.stereotype.Controller")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Service")
      || clazzMeta.isAnnotatedWith("org.springframework.stereotype.Repository");
  }
  
  private static boolean isScopePrototype(SymbolMetadata clazzMeta) {
    List<AnnotationValue> values = clazzMeta.valuesForAnnotation(SCOPE_ANNOTATION_FQN);
    if (values == null) { 
    	return false;
    }
    
    for (AnnotationValue annotationValue : values) {
      if (("value".equals(annotationValue.name()) || "scopeName".equals(annotationValue.name()))
        && annotationValue.value() instanceof LiteralTree
        && !"\"prototype\"".equals(((LiteralTree) annotationValue.value()).value())) {
        return false;
      }
    }
    return true;
  }
}
