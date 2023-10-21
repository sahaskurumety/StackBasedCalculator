

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;

/**
 * This class uses two stacks to evaluate an infix arithmetic expression from an
 * InputStream. It should not create a full postfix expression along the way; it
 * should convert and evaluate in a pipelined fashion, in a single pass.
 */
public class InfixCalculator {
    // Tokenizer to break up our input into tokens
    StreamTokenizer tokenizer;

    // Stacks for operators (for converting to postfix) and operands (for
    // evaluating)
    StackInterface<Character> operatorStack;
    StackInterface<Double> operandStack;
    
    boolean isOperand = false;
    
    int openDelims = 0;

    /**
     * Initializes the calculator to read an infix expression from an input
     * stream.
     * @param input the input stream from which to read the expression
     */
    public InfixCalculator(InputStream input) {
        // Initialize the tokenizer to read from the given InputStream
        tokenizer = new StreamTokenizer(new BufferedReader(
                        new InputStreamReader(input)));

        // StreamTokenizer likes to consider - and / to have special meaning.
        // Tell it that these are regular characters, so that they can be parsed
        // as operators
        tokenizer.ordinaryChar('-');
        tokenizer.ordinaryChar('/');

        // Allow the tokenizer to recognize end-of-line, which marks the end of
        // the expression
        tokenizer.eolIsSignificant(true);

        // Initialize the stacks
        operatorStack = new ArrayStack<Character>();
        operandStack = new ArrayStack<Double>();
    }

    /**
     * Parses and evaluates the expression read from the provided input stream,
     * then returns the resulting value
     * @return the value of the infix expression that was parsed
     */
    public Double evaluate() throws InvalidExpressionException {
        // Get the first token. If an IO exception occurs, replace it with a
        // runtime exception, causing an immediate crash.
        try {
            tokenizer.nextToken();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Continue processing tokens until we find end-of-line
        while (tokenizer.ttype != StreamTokenizer.TT_EOL) {
            // Consider possible token types
            switch (tokenizer.ttype) {
                case StreamTokenizer.TT_NUMBER:
                    // If the token is a number, process it as a double-valued
                    // operand
                    handleOperand((double)tokenizer.nval);
                    break;
                case '+':
                case '-':
                case '*':
                case '%':
                case '/':
                case '\\':
                case '^':
                    // If the token is any of the above characters, process it
                    // is an operator
                    handleOperator((char)tokenizer.ttype);
                    break;
                case '(':
                case '[':
                    // If the token is open bracket, process it as such. Forms
                    // of bracket are interchangeable but must nest properly.
                    handleOpenBracket((char)tokenizer.ttype);
                    break;
                case ')':
                case ']':
                    // If the token is close bracket, process it as such. Forms
                    // of bracket are interchangeable but must nest properly.
                    handleCloseBracket((char)tokenizer.ttype);
                    break;
                case StreamTokenizer.TT_WORD:
                    // If the token is a "word", throw an expression error
                    throw new InvalidExpressionException("Unrecognized symbol: " +
                                    tokenizer.sval);
                default:
                    // If the token is any other type or value, throw an
                    // expression error
                    throw new InvalidExpressionException("Unrecognized symbol: " +
                                    String.valueOf((char)tokenizer.ttype));
            }

            // Read the next token, again converting any potential IO exception
            try {
                tokenizer.nextToken();
            } catch(IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Almost done now, but we may have to process remaining operators in
        // the operators stack
        handleRemainingOperators();

        // Return the result of the evaluation
        return operandStack.pop();
    }

    /**
     * This method is called when the calculator encounters an operand. It
     * manipulates operatorStack and/or operandStack to process the operand
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param operand the operand token that was encountered
     */
    void handleOperand(double operand) {
    	if(isOperand) throw new InvalidExpressionException("Cannot have two operands in a row");
    	
    	operandStack.push(operand);
    	isOperand = true;
    	
    }

    /**
     * This method is called when the calculator encounters an operator. It
     * manipulates operatorStack and/or operandStack to process the operator
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param operator the operator token that was encountered
     */
    void handleOperator(char operator) {
    	if(operandStack.isEmpty()&& operator == '-') {
    		operandStack.push(0.0);
    		isOperand = true;
    	}
    	if(operandStack.isEmpty()) throw new InvalidExpressionException("Cannot begin infix expression with an operator");
    	if(!isOperand) throw new InvalidExpressionException("Cannot have two operators in a row");
    	isOperand = false;
    	
    	if(operatorStack.isEmpty()) {
    		operatorStack.push(operator);
    		return;
    	}
    	
    	
    	if(!shouldPop(operator,operatorStack.peek())){
    		operatorStack.push(operator);
    		return;
    	}
    	else {
    		while(!operatorStack.isEmpty() && shouldPop(operator,operatorStack.peek())) {
    			evaluatePop(operatorStack.pop());
    			
    		}
    		operatorStack.push(operator);
    	}
    	
    }

    /**
     * This method is called when the calculator encounters an open bracket. It
     * manipulates operatorStack and/or operandStack to process the open bracket
     * according to the Infix-to-Postfix and Postfix-evaluation algorithms.
     * @param openBracket the open bracket token that was encountered
     */
    void handleOpenBracket(char openDelim) {
    	if(isOperand && !operandStack.isEmpty()) {   //additional function: evaluate operator before delimiter as multiplication
    		operatorStack.push('*');
    	}
    	isOperand = false;
    	operatorStack.push(openDelim);
    	openDelims++;
    }

    /**
     * This method is called when the calculator encounters a close bracket. It
     * manipulates operatorStack and/or operandStack to process the close
     * bracket according to the Infix-to-Postfix and Postfix-evaluation
     * algorithms.
     * @param closeBracket the close bracket token that was encountered
     */
    void handleCloseBracket(char closeDelim) {
    	if(openDelims == 0) throw new InvalidExpressionException("Too many closing delimiters");
    	//if(closeDelim == ')' && acceptCloser == 0) throw new InvalidExpressionException("Cannot close bracket with parenthesis");
    	//if(closeDelim == ')' && acceptCloser == 0) throw new InvalidExpressionException("Cannot close parenthesis with bracket");
    	
    	while(!operatorStack.isEmpty()) {
    		//first check for invalid openers
    		if(operatorStack.isEmpty()) throw new InvalidExpressionException("Not enough opening delimiters");
    		char topOperator = operatorStack.peek();
    		if(closeDelim==')' && topOperator == '[') throw new InvalidExpressionException("Cannot close bracket with parenthesis");
    		if(closeDelim==']' && topOperator == '(') throw new InvalidExpressionException("Cannot close parenthesis with bracket");
    		
    		//check for matching opening delimiter. If it is one, pop off stack without evaluating. then return.
    		if(closeDelim==')' && topOperator == '(') {
    			operatorStack.pop();
    			openDelims--;
    			return;
    		}
    		if(closeDelim==']' && topOperator == '[') {
    			operatorStack.pop();
    			openDelims--;
    			return;
    		}
    		
    		// else, it must be a valid operator inside the delimiters. pop off and evaluate.
    		evaluatePop(operatorStack.pop());
    		
    		
    	}
    }

    /**
     * This method is called when the calculator encounters the end of an
     * expression. It manipulates operatorStack and/or operandStack to process
     * the operators that remain on the stack, according to the Infix-to-Postfix
     * and Postfix-evaluation algorithms.
     */
    void handleRemainingOperators() {
    	if(openDelims != 0 ) throw new InvalidExpressionException("Too many opening delimiters");
    	while(!operatorStack.isEmpty()) {
    		evaluatePop(operatorStack.pop());
    	}
        // TODO: Complete this method
    }


    /**
     * Creates an InfixCalculator object to read from System.in, then evaluates
     * its input and prints the result.
     * @param args not used
     */
    public static void main(String[] args) {
        System.out.println("Infix expression:");
        InfixCalculator calculator = new InfixCalculator(System.in);
        Double value = null;
        try {
            value = calculator.evaluate();
        } catch (InvalidExpressionException e) {
            System.out.println("Invalid expression: " + e.getMessage());
        }
        if (value != null) {
            System.out.println(value);
        }
    }
    
    //-------------PRIVATE METHODS---------------------
    
    private boolean shouldPop(char current, char topStack) {  // returns true if equal or lower precedence; 
    	return opPrecedence(current)<=opPrecedence(topStack);			// should pop stack if true
    }
    
    private int opPrecedence(char operator) {
    	if(operator == '^') return 3;
    	if(operator == '%' || operator == '/' || operator == '*'|| operator =='\\') return 2;
    	if(operator == '+' || operator == '-') return 1;
    	else return 0;
    }
    
    private void evaluatePop(char operator) {
    	double first = operandStack.pop();
    	double second = operandStack.pop();
    	if(operator == '*') {
    		operandStack.push(second*first);
    	}
    	if(operator == '/') {
    		operandStack.push(second/first);
    	}
    	if(operator == '\\') {//integer division
    		operandStack.push((double)((int)(second/first)));
    	}
    	if(operator == '%') {
    		operandStack.push(second%first);
    	}
    	if(operator == '+') {
    		operandStack.push(second+first);
    	}
    	if(operator == '-') {
    		operandStack.push(second-first);
    	}
    	if(operator == '^') {
    		operandStack.push(Math.pow(second, first));
    	}

    }
    

}

