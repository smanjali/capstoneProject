package edu.montana.csci.csci468.demo;

import java.io.Console;
import java.util.*;

public class RPNCalc {

    LinkedList<Integer> values =new LinkedList<>();
    Map<String, Integer> vars = new HashMap<>();

    @Override
    public String toString() {
        return values.toString() + "\n\n vars: " + vars;
    }

    public static void main(String[] args) {
        RPNCalc calc = new RPNCalc();

        Scanner scanner = new Scanner(System.in);
        System.out.print("rpn (q to quit) > ");
        String line = scanner.nextLine();

        while (!line.trim().equals("q")) {
            calc.eval(line);
            System.out.println(calc);
            System.out.print("rpn (q to quit) > ");
            line = scanner.nextLine();
        }
    }

    private void eval(String src) {
        String[] strs = src.split(" ");
        for (int i = 0; i < strs.length; i++) {
            String str = strs[i];
            try {
                int val = Integer.parseInt(str);
                values.push(val);
                continue;
            } catch (NumberFormatException e) {
                // continue
            }
            if ("->".equals(str)) {
                i = i + 1;
                String name = strs[i];
                vars.put(name, values.pop());
            } else if (str.matches("[a-z]")) {
                values.push(vars.get(str));
            } else if ("+".equals(str)) {
                Integer val1 = values.pop();
                Integer val2 = values.pop();
                values.push(val1 + val2);
            }
        }
    }

}
