package plc.project;

import jdk.nashorn.internal.parser.TokenLookup;

import javax.lang.model.type.NullType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 *
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 *
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globals = new ArrayList<Ast.Global>();
        List<Ast.Function> funcs = new ArrayList<Ast.Function>();
        while(peek("LIST") || peek("VAR") || peek("VAL")) {
            globals.add(parseGlobal());
        }
        while(peek("FUN")) {
            funcs.add(parseFunction());
        }
        return new Ast.Source(globals, funcs);
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a global, aka {@code LIST|VAL|VAR}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global global = null;
        if(peek("LIST")) {
            global = parseList();
        }
        else if(peek("VAR")) {
            global = parseMutable();
        }
        else if(peek("VAL")) {
            global = parseImmutable();
        }

        if (match(";")) {
            return global;
        }

        if (tokens.has(0)) {
            throw new ParseException("Expected ';':", tokens.get(0).getIndex());
        } else {
            throw new ParseException("Expected ';':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }

    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        match("LIST");
        if(peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (!match("=")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected '=':", tokens.get(0).getIndex());
                } else {
                    throw new ParseException("Expected '=':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }

            if (!match("[")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected '=':", tokens.get(0).getIndex());
                } else {
                    throw new ParseException("Expected '=':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            List<Ast.Expression> values = new ArrayList<Ast.Expression>();
            values.add(parseExpression());
            if(match(",")) {
                while (!match("]")) {
                    values.add(parseExpression());

                    if (!match(",")) {
                        if (!peek("]")) {
                            if (tokens.has(0)) {
                                throw new ParseException("Expected ',':", tokens.get(0).getIndex());
                            } else {
                                throw new ParseException("Expected ']':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                            }
                        }
                    }
                    else {
                        if (peek("]")) {
                            throw new ParseException("Expected parameter:", tokens.get(0).getIndex());
                        }
                    }
                }
            }
            if(!match("]")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected ']':", tokens.get(0).getIndex());
                } else {
                    throw new ParseException("Expected ']':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }

            return new Ast.Global(name, true, Optional.of(new Ast.Expression.PlcList(values)));
        }
        else {
            if (tokens.has(0)) {
                throw new ParseException("Expected Identifier:", tokens.get(0).getIndex());
            } else {
                throw new ParseException("Expected Identifier:", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        match("VAR");
        if(peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (match("=")) {
                return new Ast.Global(name, true, Optional.of(parseExpression()));
            }
            return new Ast.Global(name, true, Optional.empty());
        }
        else {
            if (tokens.has(0)) {
                throw new ParseException("Expected Identifier:", tokens.get(0).getIndex());
            } else {
                throw new ParseException("Expected Identifier:", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        match("VAL");
        if(peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (!match("=")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected '=':", tokens.get(0).getIndex());
                } else {
                    throw new ParseException("Expected '=':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            return new Ast.Global(name, false, Optional.of(parseExpression()));
        }
        else {
            if (tokens.has(0)) {
                throw new ParseException("Expected Identifier:", tokens.get(0).getIndex());
            } else {
                throw new ParseException("Expected Identifier:", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses the {@code function} rule. This method should only be called if the
     * next tokens start a method, aka {@code FUN}.
     */
    public Ast.Function parseFunction() throws ParseException {
        match("FUN");
        if(peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            List<String> parameters = new ArrayList<String>();
            if(!match("(")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected '(':", tokens.get(0).getIndex());
                } else {
                    throw new ParseException("Expected '(':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }

            while(!match(")")) {
                if(peek(Token.Type.IDENTIFIER)) {
                    parameters.add(tokens.get(0).getLiteral());
                }
                else {
                    if (tokens.has(0)) {
                        throw new ParseException("Expected Parameter:", tokens.get(0).getIndex());
                    } else {
                        throw new ParseException("Expected Parameter:", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                }
                if(!match(",")) {
                    if(!peek(")")) {
                        if(tokens.has(0)) {
                            throw new ParseException("Expected ',':", tokens.get(0).getIndex());
                        }
                        else {
                            throw new ParseException("Expected ')':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }
                    }
                }
                else {
                    if(peek(")")) {
                        throw new ParseException("Expected parameter:", tokens.get(0).getIndex());
                    }
                }
            }

            if(!match("DO")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected 'DO':", tokens.get(0).getIndex());
                } else {
                    throw new ParseException("Expected 'DO':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }

            List<Ast.Statement> body = parseBlock();
            if(!match("END")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected 'END':", tokens.get(0).getIndex());
                } else {
                    throw new ParseException("Expected 'END':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            return new Ast.Function(name, parameters, body);
        }
        else {
            if(tokens.has(0)) {
                throw new ParseException("Expected Function Name:", tokens.get(0).getIndex());
            }
            else {
                throw new ParseException("Expected Function Name:", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<Ast.Statement>();
        while(!peek("END") && !peek("ELSE")) {
            if(!tokens.has(0)) {
                throw new ParseException("Unterminated Block:", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
            else if(peek("LIST") || peek("VAR") || peek("VAL") || peek("FUN")) {
                throw new ParseException("Unexpected Token:", tokens.get(0).getIndex());
            }
            statements.add(parseStatement());
        }
        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if(peek("LET")) {
            return parseDeclarationStatement();
        }
        else if(peek("SWITCH")) {
            return parseSwitchStatement();
        }
        else if(peek("IF")) {
            return parseIfStatement();
        }
        else if(peek("WHILE")) {
            return parseWhileStatement();
        }
        else if(peek("RETURN")) {
            return parseReturnStatement();
        }
        else {
            Ast.Statement statement;
            Ast.Expression left = parseExpression();

            if (match("=")) {
                Ast.Expression right = parseExpression();
                statement = new Ast.Statement.Assignment(left, right);
            } else {
                statement = new Ast.Statement.Expression(left);
            }

            if (match(";")) {
                return statement;
            }

            if (tokens.has(0)) {
                throw new ParseException("Expected ';':", tokens.get(0).getIndex());
            } else {
                throw new ParseException("Expected ';':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        Ast.Statement.Declaration statement;
        match("LET");
        if(peek(Token.Type.IDENTIFIER)) {
            String name = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if(match("=")) {
                statement = new Ast.Statement.Declaration(name, Optional.of(parseExpression()));
            }
            else {
                statement = new Ast.Statement.Declaration(name, Optional.empty());
            }

            if (match(";")) {
                return statement;
            }

            if (tokens.has(0)) {
                throw new ParseException("Expected ';':", tokens.get(0).getIndex());
            }
            else {
                throw new ParseException("Expected ';':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
        else {
            if (tokens.has(0)) {
                throw new ParseException("Expected Identifier:", tokens.get(0).getIndex());
            } else {
                throw new ParseException("Expected Identifier:", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        Ast.Statement.If statement;
        match("IF");
        Ast.Expression condition = parseExpression();
        if(match("DO")) {
            List<Ast.Statement> ifBlock = parseBlock();
            if(match("ELSE")) {
                List<Ast.Statement> elseBlock = parseBlock();
                statement = new Ast.Statement.If(condition, ifBlock, elseBlock);
            }
            else {
                statement = new Ast.Statement.If(condition, ifBlock, new ArrayList<Ast.Statement>());
            }
            if(!match("END")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected 'END':", tokens.get(0).getIndex());
                } else {
                    throw new ParseException("Expected 'END':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            return statement;
        }
        else {
            if (tokens.has(0)) {
                throw new ParseException("Expected 'DO':", tokens.get(0).getIndex());
            } else {
                throw new ParseException("Expected 'DO':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        match("SWITCH");
        Ast.Expression switchExpr = parseExpression();
        List<Ast.Statement.Case> caseList = new ArrayList<Ast.Statement.Case>();
        while (peek("CASE")) {
            caseList.add(parseCaseStatement());
        }
        if(peek("Default")) {
            caseList.add(parseCaseStatement());
            if(!match("END")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected 'END':", tokens.get(0).getIndex());
                }
                else {
                    throw new ParseException("Expected 'END':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            return new Ast.Statement.Switch(switchExpr, caseList);
        }
        else {
            if (tokens.has(0)) {
                throw new ParseException("Expected 'DEFAULT':", tokens.get(0).getIndex());
            }
            else {
                throw new ParseException("Expected 'DEFAULT':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        if (match("CASE")) {
            Ast.Expression caseExpr = parseExpression();
            if(!match(":")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected ':':", tokens.get(0).getIndex());
                }
                else {
                    throw new ParseException("Expected ':':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            return new Ast.Statement.Case(Optional.of(caseExpr), parseBlock());
        }
        else  {
            match("DEFAULT");
            return new Ast.Statement.Case(Optional.empty(), parseBlock());
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expression condition = parseExpression();
        if (match("DO")) {
            List<Ast.Statement> block = parseBlock();
            if(!match("END")) {
                if (tokens.has(0)) {
                    throw new ParseException("Expected 'END':", tokens.get(0).getIndex());
                }
                else {
                    throw new ParseException("Expected 'END':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                }
            }
            return new Ast.Statement.While(condition, block);
        }
        else {
            if (tokens.has(0)) {
                throw new ParseException("Expected 'DO':", tokens.get(0).getIndex());
            }
            else {
                throw new ParseException("Expected 'DO':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
            }
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");
        Ast.Expression expr = parseExpression();

        if (match(";")) {
            return new Ast.Statement.Return(expr);
        }

        if (tokens.has(0)) {
            throw new ParseException("Expected ';':", tokens.get(0).getIndex());
        }
        else {
            throw new ParseException("Expected ';':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Token t = null;
        Ast.Expression left = parseComparisonExpression();
        if(tokens.has(0)) {
            t = tokens.get(0);
        }
        if(match("&&") || match("||")) {
            Ast.Expression right = parseLogicalExpression();
            return new Ast.Expression.Binary(t.getLiteral(), left, right);
        }
        else {
            return left;
        }
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Token t = null;
        Ast.Expression left = parseAdditiveExpression();
        if(tokens.has(0)) {
            t = tokens.get(0);
        }
        if(match("<") || match(">") || match("==") || match("!=")) {
            Ast.Expression right = parseComparisonExpression();
            return new Ast.Expression.Binary(t.getLiteral(), left, right);
        }
        else {
            return left;
        }
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Token t = null;
        Ast.Expression left = parseMultiplicativeExpression();
        if(tokens.has(0)) {
            t = tokens.get(0);
        }
        if(match("+") || match("-")) {
            Ast.Expression right = parseAdditiveExpression();
            return new Ast.Expression.Binary(t.getLiteral(), left, right);
        }
        else {
            return left;
        }
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Token t = null;
        Ast.Expression left = parsePrimaryExpression();
        if(tokens.has(0)) {
            t = tokens.get(0);
        }
        if(match("*") || match("/") || match("^")) {
            Ast.Expression right = parseMultiplicativeExpression();
            return new Ast.Expression.Binary(t.getLiteral(), left, right);
        }
        else {
            return left;
        }
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */

    public Ast.Expression parsePrimaryExpression() throws ParseException {
        Token t = null;
        if(tokens.has(0)) {
            t = tokens.get(0);

            if (match("TRUE") || match("FALSE"))
                return new Ast.Expression.Literal(new Boolean(t.getLiteral()));
            else if (match("NIL"))
                return new Ast.Expression.Literal(null);
            else if (match(Token.Type.INTEGER))
                return new Ast.Expression.Literal(new BigInteger(t.getLiteral()));
            else if (match(Token.Type.DECIMAL))
                return new Ast.Expression.Literal(new BigDecimal(t.getLiteral()));
            else if (match(Token.Type.CHARACTER)) {
                String literal = t.getLiteral().substring(1, t.getLiteral().length() - 1); //removes ''
                switch(literal) {
                    case "\\n": literal = literal.replace("\\n", "\n"); break;
                    case "\\b": literal = literal.replace("\\b", "\b"); break;
                    case "\\r": literal = literal.replace("\\r", "\r"); break;
                    case "\\t": literal = literal.replace("\\t", "\t"); break;
                    case "\\'": literal = literal.replace("\\'", "\'"); break;
                    case "\\\"": literal = literal.replace("\\\"", "\""); break;
                    case "\\\\": literal = literal.replace("\\\\", "\\"); break;
                }
                return new Ast.Expression.Literal(new Character(literal.charAt(0)));
            }
            else if (match(Token.Type.STRING)) {
                String literal = t.getLiteral().substring(1, t.getLiteral().length() - 1); //removes ""
                literal = literal.replace("\\n", "\n");
                literal = literal.replace("\\b", "\b");
                literal = literal.replace("\\r", "\r");
                literal = literal.replace("\\t", "\t");
                literal = literal.replace("\\'", "\'");
                literal = literal.replace("\\\"", "\"");
                literal = literal.replace("\\\\", "\\");

                return new Ast.Expression.Literal(literal);
            }
            else if(match("(")) {
                Ast.Expression grouped = new Ast.Expression.Group(parseExpression());
                if(!match(")")) {
                    if(tokens.has(0)) {
                        throw new ParseException("Expected ')':", tokens.get(0).getIndex());
                    }
                    else {
                        throw new ParseException("Expected ')':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                    }
                }
                return grouped;
            }
            else if(match(Token.Type.IDENTIFIER)) {
                if(match("[")) {
                    Ast.Expression access = new Ast.Expression.Access(Optional.of(parseExpression()), t.getLiteral());
                    if(!match("]")) {
                        if(tokens.has(0)) {
                            throw new ParseException("Expected ']':", tokens.get(0).getIndex());
                        }
                        else {
                            throw new ParseException("Expected ']':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                        }
                    }
                    return access;
                }
                else if(match("(")) {
                    List<Ast.Expression> parameters = new ArrayList<Ast.Expression>();
                    while(!match(")")) {
                        parameters.add(parseExpression());
                        if(!match(",")) {
                            if(!peek(")")) {
                                if(tokens.has(0)) {
                                    throw new ParseException("Expected ',':", tokens.get(0).getIndex());
                                }
                                else {
                                    throw new ParseException("Expected ')':", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
                                }
                            }
                        }
                        else {
                            if(peek(")")) {
                                throw new ParseException("Expected parameter:", tokens.get(0).getIndex());
                            }
                        }
                    }
                    return new Ast.Expression.Function(t.getLiteral(), parameters);
                }
                else {
                    return new Ast.Expression.Access(Optional.empty(), t.getLiteral());
                }
            }
        }
        if(tokens.has(0)) {
            throw new ParseException("Unknown token:", tokens.get(0).getIndex());
        }
        else {
            throw new ParseException("Expected token:", tokens.get(-1).getIndex() + tokens.get(-1).getLiteral().length());
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     *
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for(int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i))
                return false;
            else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType())
                    return false;
            }
            else if(patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral()))
                    return false;
            }
            else
                throw new AssertionError("Invalid pattern object: " + patterns[i].getClass());
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);

        if(peek) {
            for(int i = 0; i < patterns.length; i++)
                tokens.advance();
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
