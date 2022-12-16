package plc.project;
import javax.lang.model.type.NullType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Void> {

    public Scope scope;
    private Ast.Function function;

    public Analyzer(Scope parent) {
        scope = new Scope(parent);
        scope.defineFunction("print", "System.out.println", Arrays.asList(Environment.Type.ANY), Environment.Type.NIL, args -> Environment.NIL);
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Void visit(Ast.Source ast) {

        for(int i=0; i<ast.getGlobals().size(); i++)
        {
            visit(ast.getGlobals().get(i));
        }
        for(int k=0; k<ast.getFunctions().size(); k++)
        {
            visit(ast.getFunctions().get(k));
        }

        requireAssignable(Environment.Type.INTEGER, scope.lookupFunction("main", 0).getReturnType());
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {

        if(ast.getValue().isPresent())
        {
            visit(ast.getValue().get());
            requireAssignable(Environment.getType(ast.getTypeName()), ast.getValue().get().getType());
        }
        ast.setVariable(new Environment.Variable(ast.getName(),
                ast.getName(),
                Environment.getType(ast.getTypeName()),
                ast.getMutable(),
                Environment.NIL));

        scope.defineVariable(ast.getName(),
                ast.getName(),
                ast.getVariable().getType(),
                ast.getMutable(),
                ast.getVariable().getValue());

        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {

        List<Environment.Type> params = new ArrayList();
        
        for(int i=0; i<ast.getParameterTypeNames().size(); i++)
        {
            params.add(Environment.getType(ast.getParameterTypeNames().get(i)));
        }

        Environment.Type returnT = Environment.Type.NIL;
        if(ast.getReturnTypeName().isPresent())
        {
            returnT = Environment.getType(ast.getReturnTypeName().get());
        }

        ast.setFunction(new Environment.Function(ast.getName(),
                ast.getName(),
                params,
                returnT,
                args -> Environment.NIL
                ));

        scope.defineFunction(
                ast.getName(),
                ast.getName(),
                params,
                returnT,
                args -> Environment.NIL);

        this.scope = new Scope(scope);
        for (int j = 0; j < ast.getParameters().size(); j++)
        {

            scope.defineVariable(
                    ast.getParameters().get(j),
                    ast.getParameters().get(j),
                    params.get(j),
                    true,
                    Environment.NIL
                    );
        }

        for(int k=0; k<ast.getStatements().size(); k++)
        {
            visit(ast.getStatements().get(k));
            if(ast.getStatements().get(k) instanceof Ast.Statement.Return)
            {
                requireAssignable(((Ast.Statement.Return) ast.getStatements().get(k)).getValue().getType(), returnT);
            }
        }

        this.scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        if(!(ast.getExpression() instanceof Ast.Expression.Function))
        {
            throw new RuntimeException();
        }
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast)
    {
        if(ast.getValue().isPresent()) {
            visit(ast.getValue().get());
            if(ast.getTypeName().isPresent())
            {
                requireAssignable(Environment.getType(ast.getTypeName().get()), ast.getValue().get().getType());
            }
        }

        Environment.Type TypeNom = Environment.Type.NIL;
        if (ast.getTypeName().isPresent())
        {
            visit(ast.getValue().get());
            TypeNom = Environment.getType(ast.getTypeName().get());
        }
        else if(ast.getValue().isPresent())
        {
            TypeNom = ast.getValue().get().getType();
        }
        else
        {
            throw new RuntimeException();
        }

        ast.setVariable(new Environment.Variable(
                ast.getName(),
                ast.getName(),
                TypeNom,
                true,
                Environment.NIL
        ));

        scope.defineVariable(
                ast.getName(),
                ast.getName(),
                TypeNom,
                true,
                Environment.NIL

        );

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        visit(ast.getReceiver());
        visit(ast.getValue());
        requireAssignable(ast.getReceiver().getType(), ast.getValue().getType());   //Error

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        if(ast.getThenStatements().isEmpty())
            throw new RuntimeException();

        visit(ast.getCondition());
        requireAssignable(ast.getCondition().getType(), Environment.Type.BOOLEAN);

        //System.out.println("checkpoint 1");

        this.scope = new Scope(scope);
        for(int i=0; i<ast.getThenStatements().size(); i++)
        {
            visit(ast.getThenStatements().get(i));
        }

        //System.out.println("checkpoint 2");

        for(int i=0; i<ast.getElseStatements().size(); i++)
        {
            visit(ast.getElseStatements().get(i));
        }

        //System.out.println("checkpoint 3");

        this.scope = scope.getParent();
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {

        visit(ast.getCondition());
        for (int i = 0; i < ast.getCases().size(); i++) {
            if(ast.getCases().get(i).getValue().isPresent()) {
                visit(ast.getCases().get(i).getValue().get());
                requireAssignable(ast.getCondition().getType(), ast.getCases().get(i).getValue().get().getType());
            }
        }
        if(ast.getCases().get(ast.getCases().size()-1).getValue().isPresent())
        {
            throw new RuntimeException();
        }

        //visit(ast.getCondition());

        for (int k = 0; k < ast.getCases().size(); k++) {
            this.scope = new Scope(scope);
            visit(ast.getCases().get(k));
            this.scope = scope.getParent();
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        for(int i=0; i<ast.getStatements().size(); i++)
        {
            this.scope = new Scope(scope);
            visit(ast.getStatements().get(i));
            this.scope = scope.getParent();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        requireAssignable(Environment.Type.BOOLEAN, ast.getCondition().getType());

        this.scope = new Scope(scope);
        for(int i=0; i<ast.getStatements().size(); i++)
        {
            visit(ast.getStatements().get(i));
        }
        this.scope = scope.getParent();

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        visit(ast.getValue());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        //throw new UnsupportedOperationException();
        if(ast.getLiteral() instanceof Boolean)
        {
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(ast.getLiteral() instanceof NullType)
        {
            ast.setType(Environment.Type.NIL);
        }
        else if(ast.getLiteral() instanceof Character)
        {
            ast.setType(Environment.Type.CHARACTER);
        }
        else if(ast.getLiteral() instanceof String)
        {
            ast.setType(Environment.Type.STRING);
        }
        else if(ast.getLiteral() instanceof BigInteger)
        {
            ast.setType(Environment.Type.INTEGER);
            if(((BigInteger) ast.getLiteral()).bitCount() > 32)
                throw new RuntimeException();

        }
        else if(ast.getLiteral() instanceof BigDecimal)
        {
            ast.setType(Environment.Type.DECIMAL);
            double d = ((BigDecimal)ast.getLiteral()).doubleValue();
            BigDecimal bdm = new BigDecimal(Double.MAX_VALUE);
            //((BigDecimal) ast.getLiteral()).compareTo(bdm) == 1
            if (Double.isInfinite(d))
            {
                throw new RuntimeException();
            }
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        visit(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        visit(ast.getRight());
        visit(ast.getLeft());
        if(ast.getOperator() == "&&" || ast.getOperator() == "||")
        {
            requireAssignable(ast.getLeft().getType(), Environment.Type.BOOLEAN);
            requireAssignable(ast.getRight().getType(), Environment.Type.BOOLEAN);
            ast.setType(Environment.Type.BOOLEAN);
        }
        else if(ast.getOperator() == "<" || ast.getOperator() == ">" || ast.getOperator() == "==" || ast.getOperator() == "!=")
        {
            requireAssignable(ast.getLeft().getType(), Environment.Type.COMPARABLE);
            requireAssignable(ast.getRight().getType(), Environment.Type.COMPARABLE);

            if(ast.getLeft().getType().equals(ast.getRight().getType()))
            {
                ast.setType(Environment.Type.BOOLEAN);
            }
            else
                throw new RuntimeException();
        }
        else if(ast.getOperator() == "+")
        {
            if (!(ast.getLeft().getType().equals(Environment.Type.STRING) || ast.getRight().getType().equals(Environment.Type.STRING)))
            {
                if(ast.getLeft().getType().equals(Environment.Type.DECIMAL) || ast.getLeft().getType().equals(Environment.Type.INTEGER))
                {
                    requireAssignable(ast.getLeft().getType(), ast.getRight().getType()); //    checking if left and right are the same
                    ast.setType(ast.getLeft().getType());   //  setting (decorating) ast node as same type as left
                }
                else
                    throw new RuntimeException("lhs is not decimal/int/string");
            }
            else
            {
                ast.setType(Environment.Type.STRING);   //  both/either rhs and lhs are string, thus node is string
            }
        }
        else if(ast.getOperator() == "^")
        {
            requireAssignable(ast.getLeft().getType(), Environment.Type.INTEGER);
            requireAssignable(ast.getRight().getType(), Environment.Type.INTEGER);
            ast.setType(Environment.Type.INTEGER);
        }
        else
        {
            if(!(ast.getLeft().getType().equals(Environment.Type.DECIMAL) || ast.getLeft().getType().equals(Environment.Type.INTEGER)))
                throw new RuntimeException();
            //requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
            if(ast.getLeft().getType().equals(ast.getRight().getType()))
            {
                ast.setType(ast.getLeft().getType());
            }
            else
                throw new RuntimeException();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent())
        {
            visit(ast.getOffset().get());
        }
        if(!ast.getOffset().isPresent() || (ast.getOffset().get().getType().getName().equals(Environment.Type.INTEGER.getName())))
        {
            ast.setVariable(new Environment.Variable(ast.getName(),
                    ast.getName(),
                    scope.lookupVariable(ast.getName()).getType(),
                    true,
                    Environment.NIL));
        }
        else {
            //if(!(ast.getOffset().get().getType().getName().equals(Environment.Type.INTEGER.getName())))
                throw new RuntimeException();
        }
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        List<Environment.Type> l = new ArrayList();
        List<Environment.Type> params = scope.lookupFunction(ast.getName(), ast.getArguments().size()).getParameterTypes();

        for (int i=0; i<ast.getArguments().size(); i++)
        {
            visit(ast.getArguments().get(i));
            l.add(ast.getArguments().get(i).getType());

            requireAssignable(params.get(i), l.get(i));
        }

        ast.setFunction(scope.lookupFunction(ast.getName(), ast.getArguments().size()));

        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        for(int i=0; i<ast.getValues().size(); i++)
        {
            requireAssignable(ast.getValues().get(i).getType(), ast.getType());
        }
        return null;
    }

    public static void requireAssignable(Environment.Type target, Environment.Type type) {
        //throw new UnsupportedOperationException();  // TODO
        //
        if(!(type.getName().equals(target.getName())))
        {
            if(target.getName().equals(Environment.Type.COMPARABLE.getName()) && type.getName().equals(Environment.Type.BOOLEAN))
            {
                throw new RuntimeException();
            }
            else if(type.getName().equals(Environment.Type.COMPARABLE.getName()) && target.getName().equals(Environment.Type.BOOLEAN.getName()))
            {
                throw new RuntimeException();
            }
            else if(!(target.getName().equals(Environment.Type.COMPARABLE.getName()) || type.getName().equals(Environment.Type.COMPARABLE.getName())))
            {
                if(target.getName().equals(Environment.Type.ANY.getName()))
                {
                    return;
                }
                else
                    throw new RuntimeException();
            }
            else if((target.getName().equals(Environment.Type.ANY.getName())))
            {
                return;
            }
        }
    }

}
