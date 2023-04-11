# How to determinate Java calling method during compile time

Kevin Chen - Ringcentral Web Application Team

All Java developers have called java methods during coding, however most of the developers didn't know how exact the target method was determined during compile time and runtime. There are also some invocation form which some Java developers weren't aware of it. Java has one full set of rules to identify target method during compile time and runtime in different phases, we will focus on compile time phase in this article, runtime phase will be not in the scope.


## Java Method Invocation Expressions
There are different kinds of invocation expressions in Java, we need to learn them before we can fully understand how Java compiler identifies the calling target method. The expression itself also matter since it will impact how Java compiler interprets it.
The expressions below are the major Java method invocation expressions we need to know:

Method Invocation Expression:
* **MethodName( [ArgumentList] )**: The method name with parameters, this is the most popular method calling expression in Java, the target method can be instance method or static method. 
* **TypeName.[TypeArguments] Identifier ( [ArgumentList] )**: Calls static method.
* **ExpressionName.[TypeArguments] Identifier ( [ArgumentList] )**
* **Primary.[TypeArguments] Identifier ( [ArgumentList] )**: Similar with previous expression, it depends on the resolution of the expression before ".", the target method could be either instance or static method.
* **super.[TypeArguments] Identifier ( [ArgumentList] )**: Calls instance method inherits from superclass.
* **TypeName.super. [TypeArguments] Identifier ( [ArgumentList] )**: This is a very special method calling expression and will talk about it later in this article.

ArgumentList:<br/>
Expression {, Expression}

## Step 1: determine type to search
All Java method has to be defined inside a Java class, therefor Java compiler will need to figure out the target class as a starting point.
There are six different cases based on Java language specification, we will review them one by one:
 1. Java compiler will perform "comb rule" to find the target type if the form is **MethodName** which is the first expression form in previous section, the detail rules are：
    * Let say the class which calls the method is class E, if class E or its superclass contains either an instance or a static method, then use E as search type. 
    * If the invocation method is inside a nested class like anonymous class or inner class, and it's outer class or outer class's superclass hierarchy contains the invocation method, then use this outer class as search type.
    * Compiler will check if there are any static import method matches with invocation method if previous two phases are all failed.<br/>
    Simply put, the compiler will try with caller class inherited hierarchy, then from its outer class inherited hierarchy，at last checks if there is any static import method matches the target method. **Please be aware that Java compiler only cares about method name without checking signature in this phase.**。
 2. TypeName . [TypeArguments] Identifier: It's static method invocation, Java compiler will use TypeName as search type.
 3. ExpressionName . [TypeArguments] Identifier.
 4. Primary.[TypeArguments] Identifier: Third and fourth expression are similar, Java compiler will use the type of the expression as search type.
 5. super. [TypeArguments] Identifier: Calls the method of super class, Java compiler will search the super class hierarchy.
 6. TypeName.super. [TypeArguments] Identifier, there are two different cases：
    * If TypeName is Java class，then uses the super class of TypeName; if TypeName is Object class，Java compiler will throw compile error since Object class doesn't have super class.
    * If TypeName is Java interface，then the class of the calling method (either outer class or current class) must directly implement this Java interface, otherwise Java compiler will throw compile error, if matches the precondition then use this interface, and it's inherited hierarchy as search type.
 
## Step 2: determine Java method signature
Step 2 will be executed after step 1, it starts from the class or interface determines in step 1, there are three phases in this step in order to be backward compatible with older Java version: 
1. The first phase performs overload without permitting boxing and unboxing conversion，also not permitting the use of variable arity method invocation，compiler will stop if it finds the matched method signature otherwise continues with the next phases.
2. The second phase performs overload with permitting boxing and unboxing conversion, but still precludes the use of variable arity method invocation, if no applicable method is found during this phase then processing continues to the third phase.
3. The third phase allows overloading to be combined with variable arity methods, boxing, and unboxing.

Compiler will throw compile error if it cannot find a matched method in these three phases, let's review two examples from Java language specification：

Example 1：
```java
class ColoredPoint {
    int x, y;
    byte color;
    void setColor(byte color) { this.color = color; }
}
class Test {
    public static void main(String[] args) {
        ColoredPoint cp = new ColoredPoint();
        byte color = 37;
        cp.setColor(color);
        cp.setColor(37);  // compile-time error
    }
}
```
Java compiler cannot safely convert Integer "37" to byte and will throw compile error due to there is not matched method defined in ColoredPoint class.

Example 2：
```java
class Point { int x, y; }
class ColoredPoint extends Point { int color; }
class Test {
    static void test(ColoredPoint p, Point q) {
        System.out.println("(ColoredPoint, Point)");
    }
    static void test(Point p, ColoredPoint q) {
        System.out.println("(Point, ColoredPoint)");
    }
    public static void main(String[] args) {
        ColoredPoint cp = new ColoredPoint();
        test(cp, cp);  // compile-time error
    }
}
```
This is a typical "**overload is ambiguous**" example, **ColoredPoint** class type can be safely converted to its super class type which is **Point**，however there are two static matched methods in Test class which are **test(ColoredPoint p, Point q)** and **test(Point p, ColoredPoint q)**，Java compiler cannot determine which method is more accurate than the other and throws compile error.

Java invocation method signature including the type (class or interface) it belongs to will be not changed after compile to Java class, it might be having issue if you simply replace the target jar file your project depends on when there are method signature changed.

## Case study
Talk is cheap, let's study the following typical cases, so we can fully understand what we just learn:

Case 1：
```java
class Super {
    void f2(String s)       {}
    void f3(String s)       {}
    void f3(int i1, int i2) {}
}

class Test {
    void f1(int i) {}
    void f2(int i) {}
    void f3(int i) {}

    void m() {
        new Super() {
            {
                f1(0);  // OK, resolves to Test.f1(int)
                f2(0);  // compile-time error
                f3(0);  // compile-time error
            }
        };
    }
}
```
new Super() is an anonymous subclass of "Super" class, "Super" class contains method f2 and method f3，Test class is the outer class of the anonymous class，it contains method f1, f2 and f3.
* f1(0) matches the MethodName form，firstly compiler searches the target method in its inherited hierarchy but cannot find it, then it continues to search from its outer class which is **Test** and finds the method "f1", both method name and signature are all matched and return.
* f2(0) matches the MethodName form, compiler searches from its inherited hierarchy first and get the method "f2", so compiler will use "Super" class as starting point and completes the first step，however the method "f2" parameter type cannot match so compiler throws compile time error. **Please be aware that the two steps are independent, compiler will not go back to first step to continue if it cannot find matched method in the second step**。
* f3(0) the same with f2(0).

Question：Is it doable if we want to call **f2** of **Test** inside the anonymous class?

Case 2：
```Java
class Superclass {
    void foo() { System.out.println("Hi"); }
}

class Subclass1 extends Superclass {
    void foo() { throw new UnsupportedOperationException(); }

    Runnable tweak = new Runnable() {
        void run() {
            Subclass1.super.foo();
        }
    };
}
```
"Subclass1.super.foo()" belongs to the sixth expression form which is "TypeName.super. [TypeArguments] Identifier", Subclass1 is a Java class，so Java compiler will use its parent which is "Superclass" as starting point and match "void foo() {System.out.println("Hi");)}".

Case 3:
```Java
interface Superinterface {
    default void foo() { System.out.println("Hi"); }
}

class Subclass2 implements Superinterface {
    void foo() { throw new UnsupportedOperationException(); }

    void tweak() {
        Superinterface.super.foo(); 
    }
}
```
"Superinterface.super.foo()" also belongs to the sixth expression form which is "TypeName.super. [TypeArguments] Identifier"，the difference is "SuperInterface" is an interface not a class，this kind of expression form has a precondition that the class contains the method invocation shall directly implement this interface.
"Subclass2" direct implement "Superinterface" in this example and meets the criteria，therefor Java compiler will use "Superinterface" as starting point and find the matched default "foo" method.<br/>
Note: Compile will continue to search in the inherited system of its superinterface if "Superinterface" doesn't define default "foo" method。

Case 4：
```java
public class Tester {
    public static void calc(long value) {
        System.out.println(String.format("%s calc by calc(long value).", value));
    }

    public static void calc(Integer value) {
        System.out.println(String.format("%s calc by calc(Integer value).", value));
    }

    public static void main(String[] args) {
        calc(20);
    }
}
```
Java compiler doesn't perform boxing and unboxing according to the rules of step 2.1, "20" can be safely convert to long type and will match "calc(long value)"，Java runtime will print "20 calc by calc(long value)." which might be different with your intuitive.

Case 5：
```java
public class Tester {
    public static void calc(long value) {
        System.out.println(String.format("%s calc by calc(long value).", value));
    }

    public static void calc(Integer value) {
        System.out.println(String.format("%s calc by calc(Integer value).", value));
    }
    
    public static void calc(int value) {
        System.out.println(String.format("%s calc by calc(int value).", value));
    }
    
    public static void main(String[] args) {
        calc(20);
        calc(Integer.valueOf(20));
    }
}
```
Case 5 updates from case 4 and defines a new method "calc(int value)"，both "calc(long value)" and "calc(int value)" can match the call of "calc(20)" in step 2.1, but parameter "20" is int type so "calc(int value)" is more accurate than "calc(long value)"，compiler wil pickup the accurate one and output "20 calc by calc(int value)." when executes it.<br/>
"calc(Integer.valueOf(20))" only matches with "calc(Integer value)" and output "20 calc by calc(Integer value).".

Case 6：
```java
public class Tester {
    public static void query(Object value) {
        System.out.println("Query by static void query(Object value)");
    }

    public static void query(Object... value) {
        System.out.println("Query by static void query(Object... value)");
    }

    public static void query(String... value) {
        System.out.println("Query by static void query(String... value)");
    }
    public static void main(String[] args) {
        query(null);
        query((Object) null);
    }
}
```
The parameter "null" of the call "query(null)" is null object of any type, it can be converted to any object type, the parameter type of all three "query" method are "Object"， "Object[]" and "String[]"，"null" is able to convert to either one of them, but "Object[]" is more accurate than "Object", "String[]" is more accurate than "Object[]", therefor compiler will pickup "**query(String... value)**" method.<br/>
You can refer to [link](https://docs.oracle.com/javase/specs/jls/se17/html/jls-10.html) if you don't get why object array is more accurate than object.<br/><br/>
For "query(Object) null)", the parameter forces converting to "Object" type，so it will match "query(Object value)".

You can continue to analyze the following example and run it in your IDE to compare the result with your analyzed result if you are interested：
```Java
public class Squid {
    public void test(Object x) {
        System.out.println("in the test method that takes an object");
    }
    public void test(String x) {
        System.out.println("in the test method that takes a string");
    }
    public static void main(String[] args) {
        Squid s = new Squid();
        String x = "hi there";
        Object y = x;
        s.test(x);
        s.test(y);
        s.test( (Object) x);
    }
}
```

Java invocation method signature will be confirmed after compiled, and store in Java class file. Java runtime will use it together with invocation context during runtime to determine the final method to support Java method override, we will talk about in next article if there is a chance.

# Reference
[Java Language specification](https://docs.oracle.com/javase/specs/jls/se18/html/jls-15.html#jls-15.12)