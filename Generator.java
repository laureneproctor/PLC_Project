package plc.project;

import java.io.PrintWriter;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {
        print("Public Class Main{");

        newline(indent++);
        print("public static void main(String[] args){");
        indent++;
        newline(indent);

        indent--;
        newline(indent);
        print("}");
        return null;
    }

    @Override
    public Void visit(Ast.Global ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Function ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Switch ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Case ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Return ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {
        System.out.print("\"" + "Hello");
        if(ast.getLiteral() instanceof String)
        {
            print("\"" + ast.getLiteral() + "\"");
        }
        print(ast.getLiteral());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {
        print(ast.getExpression());
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Access ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        return null;
    }

    @Override
    public Void visit(Ast.Expression.PlcList ast) {
        return null;
    }

}
