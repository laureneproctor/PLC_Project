package plc.project;
import javax.lang.model.type.NullType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
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
        return null;
        //throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Global ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException();  // TODO
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
            if(Double.isInfinite(d))
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
            //requireAssignable(ast.getLeft().getType(), ast.getRight().getType());
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
        if(!ast.getOffset().isPresent())
        {
            ast.setVariable(new Environment.Variable(ast.getName(), ast.getName(), Environment.Type.INTEGER, true, Environment.NIL));
        }
        else
            throw new RuntimeException();
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException();  // TODO
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException();  // TODO
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
            else if((target.getName().equals(Environment.Type.ANY.getName()) || type.getName().equals(Environment.Type.ANY.getName())))
            {
                return;
            }
        }
    }

}
