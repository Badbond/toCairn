package me.soels.thesis.analysis.sources;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import me.soels.thesis.model.AbstractClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static me.soels.thesis.util.StringContainsLowerCaseMatcher.containsLowerCasedCharacters;

/**
 * Class that retrieves the required information from the given {@link ClassOrInterfaceDeclaration}.
 * <p>
 * This class halts tree traversal when an inner class has been found (unlike
 * {@link ClassOrInterfaceDeclaration#findAll(Class)}). It furthermore accumulates all required information for
 * analysis at once instead of having to traverse the tree multiple times. This not only includes traversal of child
 * nodes but also retrieval of import statement from its parent nodes.
 */
@Service
class CustomClassOrInterfaceVisitor extends VoidVisitorAdapter<CustomClassOrInterfaceVisitor.VisitorResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomClassOrInterfaceVisitor.class);

    /**
     * Visits the {@link ClassOrInterfaceDeclaration} and returns relevant results for analysis.
     * <p>
     * This includes traversal of child nodes as well as discovery of import statements accessible to this
     * {@link ClassOrInterfaceDeclaration}.
     *
     * @param node  the AST node to traverse
     * @param clazz the modeled class representing this AST node
     * @return the result of visited nodes relevant for analysis
     */
    public VisitorResult visit(ClassOrInterfaceDeclaration node, AbstractClass clazz) {
        var result = new VisitorResult(node, clazz);
        // First get accessible import declarations such that we can match them against NameExpr.
        getAccessibleImportDeclarations(node).forEach(importDecl -> visit(importDecl, result));
        // Note that we call super here as to not immediately break the visiting chain
        super.visit(result.getCallerDefinition(), result);
        return result;
    }

    private List<ImportDeclaration> getAccessibleImportDeclarations(ClassOrInterfaceDeclaration node) {
        return node.findCompilationUnit()
                .map(CompilationUnit::getImports)
                .orElse(new NodeList<>());
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration node, VisitorResult result) {
        node.getFullyQualifiedName().ifPresent(name ->
                LOGGER.trace("Stopping traversal as we found an inner class or interface declaration within to {}", name)
        );
    }

    @Override
    public void visit(MethodCallExpr node, VisitorResult result) {
        result.methodCalls.add(node);
        super.visit(node, result);
    }

    @Override
    public void visit(MethodReferenceExpr node, VisitorResult result) {
        result.methodReferences.add(node);
        super.visit(node, result);
    }

    @Override
    public void visit(ObjectCreationExpr node, VisitorResult result) {
        result.objectCreationExpressions.add(node);
        super.visit(node, result);
    }

    @Override
    public void visit(MethodDeclaration node, VisitorResult result) {
        result.declaredMethods.add(node);
        super.visit(node, result);
    }

    @Override
    public void visit(ImportDeclaration node, VisitorResult result) {
        if (node.isStatic()) {
            result.staticImports.add(node);
        } else if (!node.isAsterisk()) {
            result.regularImports.add(node);
        }
        super.visit(node, result);
    }

    @Override
    public void visit(NameExpr node, VisitorResult result) {
        if (!containsLowerCasedCharacters().matches(node.getNameAsString()) && result.staticImports.stream()
                .anyMatch(importDecl -> importDecl.getNameAsString().endsWith("." + node.getNameAsString()))) {
            result.staticNameExpressions.add(node);
        }
        super.visit(node, result);
    }

    @Override
    public void visit(FieldAccessExpr node, VisitorResult result) {
        result.fieldAccesses.add(node);
        super.visit(node, result);
    }

    static class VisitorResult {
        private final List<MethodCallExpr> methodCalls = new ArrayList<>();
        private final List<ObjectCreationExpr> objectCreationExpressions = new ArrayList<>();
        private final List<MethodReferenceExpr> methodReferences = new ArrayList<>();
        private final List<FieldAccessExpr> fieldAccesses = new ArrayList<>();
        private final List<MethodDeclaration> declaredMethods = new ArrayList<>();
        private final List<ImportDeclaration> staticImports = new ArrayList<>();
        private final List<ImportDeclaration> regularImports = new ArrayList<>();
        private final List<NameExpr> staticNameExpressions = new ArrayList<>();
        private final ClassOrInterfaceDeclaration callerDefinition;
        private final AbstractClass caller;

        public VisitorResult(ClassOrInterfaceDeclaration callerDefinition, AbstractClass caller) {
            this.callerDefinition = callerDefinition;
            this.caller = caller;
        }

        public List<MethodCallExpr> getMethodCalls() {
            return methodCalls;
        }

        public List<ObjectCreationExpr> getObjectCreationExpressions() {
            return objectCreationExpressions;
        }

        public List<MethodReferenceExpr> getMethodReferences() {
            return methodReferences;
        }

        public List<FieldAccessExpr> getFieldAccesses() {
            return fieldAccesses;
        }

        public List<ImportDeclaration> getRegularImports() {
            return regularImports;
        }

        public List<NameExpr> getStaticNameExpressions() {
            return staticNameExpressions;
        }

        public List<MethodDeclaration> getDeclaredMethods() {
            return declaredMethods;
        }

        public AbstractClass getCaller() {
            return caller;
        }

        public ClassOrInterfaceDeclaration getCallerDefinition() {
            return callerDefinition;
        }
    }
}
