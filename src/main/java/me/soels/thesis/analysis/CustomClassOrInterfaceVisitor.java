package me.soels.thesis.analysis;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class CustomClassOrInterfaceVisitor extends VoidVisitorAdapter<CustomClassOrInterfaceVisitor.InterestingNodeResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomClassOrInterfaceVisitor.class);

    public InterestingNodeResult visit(ClassOrInterfaceDeclaration node) {
        var result = new InterestingNodeResult();
        // Note that we call super here as to not immediately break the visiting chain
        super.visit(node, result);
        return result;
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration node, InterestingNodeResult result) {
        LOGGER.trace("Stopping traversal as we found an inner class or interface declaration within to {}", node.getFullyQualifiedName().get());
    }

    @Override
    public void visit(MethodCallExpr node, InterestingNodeResult result) {
        result.methodCalls.add(node);
        super.visit(node, result);
    }

    @Override
    public void visit(MethodReferenceExpr node, InterestingNodeResult result) {
        result.methodReferences.add(node);
        super.visit(node, result);
    }

    @Override
    public void visit(ObjectCreationExpr node, InterestingNodeResult result) {
        result.objectCreationExpressions.add(node);
        super.visit(node, result);
    }

    public static class InterestingNodeResult {
        private final List<MethodCallExpr> methodCalls = new ArrayList<>();
        private final List<ObjectCreationExpr> objectCreationExpressions = new ArrayList<>();
        private final List<MethodReferenceExpr> methodReferences = new ArrayList<>();

        public List<MethodCallExpr> getMethodCalls() {
            return methodCalls;
        }

        public List<ObjectCreationExpr> getObjectCreationExpressions() {
            return objectCreationExpressions;
        }

        public List<MethodReferenceExpr> getMethodReferences() {
            return methodReferences;
        }
    }
}
