package me.soels.thesis.analysis.statik;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.MethodReferenceExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import me.soels.thesis.model.AbstractClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that retrieves the required information from the given {@link ClassOrInterfaceDeclaration}.
 * <p>
 * This class halts tree traversal when an inner class has been found (unlike
 * {@link ClassOrInterfaceDeclaration#findAll(Class)}). It furthermore accumulates all required information for
 * analysis at once instead of having to traverse the tree multiple times.
 */
@Service
class CustomClassOrInterfaceVisitor extends VoidVisitorAdapter<CustomClassOrInterfaceVisitor.VisitorResult> {
    private static final Logger LOGGER = LoggerFactory.getLogger(CustomClassOrInterfaceVisitor.class);

    /**
     * Visits the child nodes of {@link ClassOrInterfaceDeclaration} and returns relevant results for analysis.
     *
     * @param node  the AST node to traverse
     * @param clazz the modeled class representing this AST node
     * @return the result of visiting child nodes relevant for analysis
     */
    public VisitorResult visit(ClassOrInterfaceDeclaration node, AbstractClass clazz) {
        var result = new VisitorResult(node, clazz);
        // Note that we call super here as to not immediately break the visiting chain
        super.visit(result.getCallerDefinition(), result);
        return result;
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

    static class VisitorResult {
        private final List<MethodCallExpr> methodCalls = new ArrayList<>();
        private final List<ObjectCreationExpr> objectCreationExpressions = new ArrayList<>();
        private final List<MethodReferenceExpr> methodReferences = new ArrayList<>();
        private final List<MethodDeclaration> declaredMethods = new ArrayList<>();
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
