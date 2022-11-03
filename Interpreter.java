package plc.project;

import javax.lang.model.element.ElementVisitor;
import java.io.ObjectInput;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

public class Interpreter implements Ast.Visitor<Environment.PlcObject> {

    private Scope scope = new Scope(null);

    public Interpreter(Scope parent) //This is a constructor
    {
        scope = new Scope(parent);
        scope.defineFunction("print", 1, args -> {
            System.out.println(args.get(0).getValue());
            return Environment.NIL;
        });

        scope.defineFunction("logarithm", 1, args -> {
            if(!(args.get(0).getValue() instanceof BigDecimal)) {
                throw new RuntimeException("Expected a BigDecimal, received " +
                        args.get(0).getValue().getClass().getName() + ".");
            }

        BigDecimal bd1 = (BigDecimal)args.get(0).getValue();
        BigDecimal bd2 = requireType(BigDecimal.class, Environment.create((args.get(0).getValue())));
        BigDecimal result = BigDecimal.valueOf(Math.log(bd2.doubleValue()));
        return Environment.create(result);


    });

        scope.defineFunction("converter", 2, args-> {
            String number = new String();
            int i, n=0;
            ArrayList<BigInteger> quotients = new ArrayList<BigInteger>();
            ArrayList<BigInteger> remainders = new ArrayList<BigInteger>();

            BigInteger base10 = requireType(BigInteger.class, Environment.create(args.get(0).getValue()));
            BigInteger base = requireType(BigInteger.class, Environment.create(args.get(1).getValue()));

            quotients.add(base10);
            do{
                quotients.add(quotients.get(n).divide(base));
                remainders.add(quotients.get(n).subtract(quotients.get(n+1).multiply(base)));
                n++;
            }
            while (quotients.get(n).compareTo(BigInteger.ZERO) > 0);
            {
                for(i = 0; i < remainders.size(); i++)
                {
                    number = remainders.get(i).toString()+number;
                }
            }
            return Environment.create(number);
        });
    }

    public Scope getScope() {
        return scope;
    }

    @Override
    public Environment.PlcObject visit(Ast.Source ast) {

        throw new UnsupportedOperationException(); //TODO
        //ast.
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {

        throw new UnsupportedOperationException(); //TODO
        //ast.
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Declaration ast)
    {
        Optional <Ast.Expression> optional = ast.getValue();
        Boolean present = optional.isPresent();

        if(present)
        {
            // cast object returned by optional.get() to an Ast.Expression type
            Ast.Expression expr = (Ast.Expression)optional.get();
            scope.defineVariable(ast.getName(), true, visit(expr));

        }
        else {
            scope.defineVariable(ast.getName(),true, Environment.NIL);
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.While ast) {
        while(requireType(Boolean.class, visit(ast.getCondition())))
        {
            try{
                scope = new Scope(scope);
                ast.getStatements().forEach(this::visit);
            }finally{
                scope = scope.getParent();
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Return ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
        //throw new UnsupportedOperationException(); //TODO
        if(ast.getLiteral() == null)
            return Environment.NIL;
        else
            return Environment.create(ast.getLiteral());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Group ast)
    {
        Ast.Expression exp = ast.getExpression();
        return visit(exp);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Binary ast) {
        Object left = visit(ast.getLeft()).getValue();
        Object right = visit(ast.getRight()).getValue();

        if(ast.getOperator().equals("&&") || ast.getOperator().equals("||"))
        {
            if((left == requireType(Boolean.class, visit(ast.getLeft()))))
            {
                if(ast.getOperator().equals("&&")) {
                    return Environment.create(Boolean.class.cast(left) && requireType(Boolean.class, visit(ast.getRight())));
                }
                else {
                    if(Boolean.class.cast(left))
                        return Environment.create(left); //  short-circuiting
                    else
                        return Environment.create(requireType(Boolean.class, visit(ast.getRight())));

                }
            }
        }
        else if(ast.getOperator().equals("<"))
        {
            if(left instanceof Comparable && right == requireType(Comparable.class, visit(ast.getRight())))
            {
                Boolean verdict = false;
                if(BigInteger.class.cast(left).compareTo(BigInteger.class.cast(right)) == -1)
                    verdict = true;
                return Environment.create(verdict);

            }
        }
        else if (ast.getOperator().equals(">"))
        {
            if(left instanceof Comparable && right == requireType(Comparable.class, visit(ast.getRight())))
            {
                Boolean verdict = false;
                if(BigInteger.class.cast(left).compareTo(BigInteger.class.cast(right)) == 1)
                    verdict = true;
                return Environment.create(verdict);
            }
        }
        else if (ast.getOperator().equals("=="))
        {
            if(left instanceof Comparable && right == requireType(Comparable.class, visit(ast.getRight())))
            {
                Boolean verdict = false;
                if(BigInteger.class.cast(left).compareTo(BigInteger.class.cast(right)) == 1)
                    verdict = true;
                return Environment.create(verdict);
            }
        }
        else if (ast.getOperator().equals("!="))
        {
            if(left instanceof Comparable && right == requireType(Comparable.class, visit(ast.getRight())))
            {
                Boolean verdict = true;
                if(BigInteger.class.cast(left).compareTo(BigInteger.class.cast(right)) == 1)
                    verdict = false;
                return Environment.create(verdict);
            }
        }
        else if(ast.getOperator().equals("+"))
        {

            if(left instanceof String || right instanceof String)
            {
                String val = "".concat(left.toString()).concat(right.toString());
                return Environment.create(val);
            }
            else {
                if(((left instanceof BigInteger) && (right instanceof BigInteger)) || ((left instanceof BigDecimal) && (right instanceof BigDecimal)))
                {
                    if(left instanceof BigInteger)
                    {
                        return Environment.create(BigInteger.class.cast(left).add(BigInteger.class.cast(right)));
                    }
                    else {
                        return Environment.create(BigDecimal.class.cast(left).add(BigDecimal.class.cast(right)));
                    }
                }
                else if(left instanceof BigInteger)
                {
                    requireType(BigInteger.class, visit(ast.getRight()));
                }
                else {
                    requireType(BigDecimal.class, visit(ast.getRight()));
                }
            }
        }
        else if(ast.getOperator().equals("-"))
        {
            if(((left instanceof BigInteger) && (right instanceof BigInteger)) || ((left instanceof BigDecimal) && (right instanceof BigDecimal)))
            {
                if(left instanceof BigInteger)
                {
                    return Environment.create(BigInteger.class.cast(left).subtract(BigInteger.class.cast(right)));
                }
                else {
                    return Environment.create(BigDecimal.class.cast(left).subtract(BigDecimal.class.cast(right)));
                }
            }
            else if(left instanceof BigInteger)
            {
                requireType(BigInteger.class, visit(ast.getRight()));
            }
            else {
                requireType(BigDecimal.class, visit(ast.getRight()));
            }
        }
        else if(ast.getOperator().equals("*"))
        {
            if(((left instanceof BigInteger) && (right instanceof BigInteger)) || ((left instanceof BigDecimal) && (right instanceof BigDecimal)))
            {
                if(left instanceof BigInteger)
                {
                    return Environment.create(BigInteger.class.cast(left).multiply(BigInteger.class.cast(right)));
                }
                else {
                    return Environment.create(BigDecimal.class.cast(left).multiply(BigDecimal.class.cast(right)));
                }
            }
            else if(left instanceof BigInteger)
            {
                requireType(BigInteger.class, visit(ast.getRight()));
            }
            else {
                requireType(BigDecimal.class, visit(ast.getRight()));
            }
        }
        else if(ast.getOperator().equals("/"))
        {
            if(((left instanceof BigInteger) && (right instanceof BigInteger)) || ((left instanceof BigDecimal) && (right instanceof BigDecimal)))
            {
                if(right == (BigInteger.ZERO))
                    throw new RuntimeException("Cannot Divide By Zero");
                else if(left instanceof BigInteger)
                {
                    return Environment.create(BigInteger.class.cast(left).divide(BigInteger.class.cast(right)));
                }
                else {
                    return Environment.create(BigDecimal.class.cast(left).divide(BigDecimal.class.cast(right), RoundingMode.HALF_EVEN));
                }
            }
            else if(left instanceof BigInteger)
            {
                requireType(BigInteger.class, visit(ast.getRight()));
            }
            else {
                requireType(BigDecimal.class, visit(ast.getRight()));
            }
        }
        else if(ast.getOperator().equals("^"))
        {
            if((left instanceof BigInteger) && (right instanceof BigInteger))
            {
                return Environment.create(BigInteger.class.cast(left).modPow(BigInteger.class.cast(right), BigInteger.ZERO));
            }
            else if(right instanceof BigInteger)
            {
                requireType(BigInteger.class, visit(ast.getLeft()));
            }
            else
            {
                requireType(BigInteger.class, visit(ast.getRight()));
            }
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Access ast) {
        if(ast.getOffset().isPresent()) {
            //Ast.Expression val = ast.getOffset().get();

            BigInteger index = BigInteger.class.cast(visit(ast.getOffset().get()).getValue());
            int i = index.intValue();
            int counter=0;
            //ArrayList.class.cast(scope.lookupVariable(ast.getName()).getValue()).get(i);

            return Environment.create(scope.lookupVariable(ast.getName()).getValue().getValue());
        }

        return Environment.create(ast.getName());
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        throw new UnsupportedOperationException(); //TODO

    }

        /**
         * Helper function to ensure an object is of the appropriate type.
         */
        private static <T> T requireType(Class<T> type, Environment.PlcObject object) {
        if (type.isInstance(object.getValue())) {
            return type.cast(object.getValue());
        } else {
            throw new RuntimeException("Expected type " + type.getName() + ", received " + object.getValue().getClass().getName() + ".");
        }
    }

    /**
     * Exception class for returning values.
     */
    private static class Return extends RuntimeException {

        private final Environment.PlcObject value;

        private Return(Environment.PlcObject value) {
            this.value = value;
        }

    }

}
