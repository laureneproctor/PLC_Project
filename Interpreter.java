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
        for(int i=0; i<ast.getGlobals().size(); i++)
        {
            visit(ast.getGlobals().get(i));
        }

        for(int k=0; k<ast.getFunctions().size(); k++)
        {
            visit(ast.getFunctions().get(k));
        }
        scope.lookupFunction("main", 0);

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Global ast) {
        if(ast.getValue().isPresent())
        {
            scope.defineVariable(ast.getName(), ast.getMutable(), visit(ast.getValue().get()));
        }
        else{
            scope.defineVariable(ast.getName(), ast.getMutable(), Environment.NIL);
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Function ast) {
        scope.defineFunction(ast.getName(), ast.getParameters().size(),(n) -> {

            scope = new Scope(scope);

            try {
                for (int i = 0; i < ast.getParameters().size(); i++) {
                    scope.defineVariable(ast.getParameters().get(i), true, n.get(i));
                }
                for (int i = 0; i < ast.getStatements().size(); i++) {
                    return visit(ast.getStatements().get(i));
                }
            }
            catch(Return r){
                return r.value;
            }

            scope=scope.getParent();
            return scope.lookupFunction(ast.getName(), ast.getParameters().size()).invoke(n);

        });

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        return Environment.NIL;
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
        //throw new UnsupportedOperationException();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Assignment ast) {
        if(ast.getReceiver() instanceof Ast.Expression.Access)
        {

            if(Ast.Expression.Access.class.cast(ast.getReceiver()).getOffset().isPresent())
            {
                Ast.Expression.Access exp = (Ast.Expression.Access)(ast.getReceiver());
                BigInteger index = BigInteger.class.cast(visit(exp.getOffset().get()).getValue());
                int i = index.intValue();
                List.class.cast(scope.lookupVariable(exp.getName()).getValue().getValue()).set(i, visit(ast.getValue()).getValue());
            }
            else {
                scope.lookupVariable(((Ast.Expression.Access) ast.getReceiver()).getName()).setValue(visit(ast.getValue()));
            }
        }
        else {
            throw new RuntimeException();
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.If ast) {
        Scope s1 = scope;
        Scope s2 = new Scope(s1);
        scope = s2;

        if(requireType(Boolean.class, visit(ast.getCondition())))
        {
                ast.getThenStatements().forEach(this::visit);
        }
        else {
            ast.getElseStatements().forEach(this::visit);
        }
        scope = s1;

        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Switch ast) {
        //throw new UnsupportedOperationException(); //TODO
        scope = new Scope(scope);
        for(int i=0; i<ast.getCases().size(); i++)
        {
            if(ast.getCases().get(i).getValue().isPresent())
            {
                //visit(ast.getCases().get(i).getValue().get());
                visit(ast.getCases().get(i));

            }

        }
        scope = scope.getParent();
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Statement.Case ast) {
        //throw new UnsupportedOperationException(); //TODO
        ast.getStatements().forEach(this::visit);
        return Environment.NIL;
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
        try{
            visit(ast.getValue());
        }
        catch(Return r)
        {
            return r.value;
        }
        return Environment.NIL;
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Literal ast) {
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
                    Boolean.class.cast(left);
                    if(right instanceof Boolean)
                        return Environment.create(Boolean.class.cast(left) || requireType(Boolean.class, visit(ast.getRight()))); //  short-circuiting
                    else {
                        return Environment.create(Boolean.class.cast(left));
                    }

                }
            }
            else
                return Environment.create(requireType(Boolean.class, visit(ast.getRight())));
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
            BigInteger index = BigInteger.class.cast(visit(ast.getOffset().get()).getValue());
            int i = index.intValue();
            return Environment.create(List.class.cast(scope.lookupVariable(ast.getName()).getValue().getValue()).get(i));
        }
        return scope.lookupVariable(ast.getName()).getValue();
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.Function ast) {
        //throw new UnsupportedOperationException(); //TODO
        List l = new ArrayList();
        for(int i=0; i<ast.getArguments().size(); i++)
        {
            l.add(visit(ast.getArguments().get(i)));
        }
        return scope.lookupFunction(ast.getName(), ast.getArguments().size()).invoke(l);
    }

    @Override
    public Environment.PlcObject visit(Ast.Expression.PlcList ast) {
        //throw new UnsupportedOperationException(); //TODO
        ArrayList gen = new ArrayList();
        for(int k=0; k<ast.getValues().size(); k++)
        {
            gen.add(visit(ast.getValues().get(k)).getValue());
        }
        return Environment.create(gen);

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
