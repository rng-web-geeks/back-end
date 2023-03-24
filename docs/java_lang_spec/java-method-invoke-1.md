# 精读Java语言规范之Java方法调用编译期规则

Kevin Chen - 铃盛软件Web Application Team

使用过Java的同学都使用过方法调用，大部分同学平常可能主要专注在如何使用，对于方法的匹配以及运行期怎么调用可能都没有太关心，实际上Java方法调用有不同的表达方式，有大家熟悉的方式，也有不太常见的方式，还有一整套解析和执行规则，简单的说，它包括两个阶段，一是编译期的方法匹配，二是执行期的方法确定和执行，本篇文章咱们先来一块探究下编译期怎么确定被调用的方法。

## Java方法调用表达式
Java的方法调用有好几种不同的表达方式，有我们熟悉的方式也有一些不常见的调用方式，要理解编译器怎么确定方法调用，需要先掌握不同的调用方式，因为不同的方式会影响到方法的解析。
按照Java语言规范的定义，以下是主要的Java方法调用格式：

MethodInvocation(方法调用):
* MethodName( [ArgumentList] ), 方法名带上调用参数，这是最常见的方法调用表达式，可以是实例方法，也可以是静态方法。 
* TypeName.[TypeArguments] Identifier ( [ArgumentList] )，调用静态方法。
* ExpressionName.[TypeArguments] Identifier ( [ArgumentList] )
* Primary.[TypeArguments] Identifier ( [ArgumentList] )，这种方式和前一种方式类似，被调用的方法取决于前面的表达式的解析结果，它可能是类静态方法调用或者实例方法调用。
* super.[TypeArguments] Identifier ( [ArgumentList] )，调用父类的实例方法。
* TypeName.super. [TypeArguments] Identifier ( [ArgumentList] )，这种方式比较特殊，留在后面再分析。

ArgumentList(参数列表):<br/>
Expression {, Expression}

## Java方法调用编译期类型确定
Java的方法在Java世界是二等公民，必须依附于某个类才能存在，所以编译器首先需要先确定从哪个类(或者接口)作为入口点来查找目标方法。
按照Java语言规范的定义，总共有6种不同的情况，这里我们逐一来分析：
 1. 如果调用形式是MethodName，(也就是上一节方法调用表达式的第一种形式)，Java编译器执行一种称为“comb rule”的查找规则，具体就是：
    * 如果方法调用所属的类或者父类存在一个方法名(静态或者实例方法)和MethodName一致，那就使用这个类作为查找入口。 
    * 或者如果方法调用处在一个嵌套类中(比如匿名类户或者内部类), 嵌套它的外部类(outer class)或者它的父类存在一个方法名和MethodName一致，则以这个外部类作为查找入口。
    * 如果上面两种都不存在，最后会检查是否有static import的方法和MethodName一致。<br/>
    简单说，就是先从自己的类继承结构查找，接着往嵌套它的外部类继承结构查找(如果有嵌套类的情况），最后看是否有static import的情况。**注意这个阶段它只关心方法名是否匹配，并不关心方法签名等其他因素，匹配到方法名就结束搜索过程**。
 2. TypeName . [TypeArguments] Identifier, 这种情况很简单，就是静态方法调用，查找入口就是TypeName指定的类型。
 3. ExpressionName . [TypeArguments] Identifier。
 4. Primary.[TypeArguments] Identifier, 第三种和第四种情况比较相似，查找对象是表达式的类型。
 5. super. [TypeArguments] Identifier, 调用父类方法，查找入口是父类及其继承结构。
 6. TypeName.super. [TypeArguments] Identifier, 分成两种情况：
    * 如果TypeName是class，那么以TypeName的直接父类作为查找入口，如果TypeName是Object，则报错（Object类没有父类）。
    * 如果TypeName是interface，那么定义代码调用所在类（嵌套的outer class，或者当前类）必须直接实现这个接口，注意间接实现是不行的，编译器会报错，如果符合条件则使用这个接口作为查找入口。
 
## Java方法调用编译期方法签名确定
第二步将在第一步的基础上进行，从第一步确定的类或接口中查找最匹配的方法签名，为了保持Java代码向下兼容，大致分成三个阶段：
1. 第一个阶段不执行装箱与拆箱操作(boxing or unboxing)，也不考虑可变参数(可变参数当做一个固定的数组参数)来查找最佳匹配方法，如果匹配到则结束，否者继续后面的步骤。
2. 第二阶段执行装箱与拆箱转换(boxing or unboxing), 但仍然把可变参数当做一个固定的数组参数来查找最佳匹配方法，如果匹配到则结束，否则继续第三阶段的操作。
3. 第三阶段允许可变参数和装箱与拆箱转换后执行最佳方法匹配。

如果三个阶段都不能匹配到最合适的方法，编译器就会抛出编译错误，下面我们看几个Java语言规范提供的例子：

例子1：
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
整数"37"并不能被安全的自动转换成byte类型，所以没有匹配到合适的方法导致编译错误。

例子2：
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
这是个典型的重载歧义(overload is ambiguous)例子, **ColoredPoint**类型可以安全的转换为它的接口类型**Point**，并且匹配到**test(ColoredPoint p, Point q)**和**test(Point p, ColoredPoint q)**，然而这两个方法区分不出来谁更精确更具体，编译器没办法确定哪个更合适，因此也会报编译错误。

Java方法调用编译期确定后，它就不会变了，编译器在生成指令的时候会包含方法所属类型和方法签名，如果你的代码的依赖包方法签名有变化，仅仅替换jar包而不重新编译工程，可能会有和预期不符的行为。

## 案例分析
光说不练假把式，掌握了理论是为了更好的应用，下面我们来一起分析几个典型案例加深理解。

案例1：
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
new Super() 实际上是一个匿名的Super类子类,它的父类有f2和f3三个方法，Test类是这个匿名类的outer类，它有f1, f2, f3三个方法。
* f1(0)属于只有MethodName的调用形式，按照上面定义的规则，它在自己的类继承结构查找不到方法f1, 会接着从嵌套它的outer类Test继续查找，并且找到f1, 方法名一致，方法签名匹配，可以正常调用。
* f2(0)属于只有MethodName的调用形式, 它在自己的继承结构找到了方法f2, 所以把Super类作为查找入口并完成第一个步骤，然后在第二个步骤方法签名匹配阶段发现参数类型不匹配，所以会报编译器错误，**注意两个步骤是独立的，这个时候并不会重复第一个步骤，到outer类Test去再次查找**。
* f3(0)的情况和f2(0)类似。

思考：假如我们在匿名类内部想调用**Test**的**f2**方法，可以怎么写?

案例2：
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
Subclass1.super.foo() 属于上面介绍的调用形式的第六种情况TypeName.super. [TypeArguments] Identifier, 这里的Subclass1是class，所以使用它的父类Superclass作为查找入口并引用到"void foo() {System.out.println("Hi");)}

案例3:
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
Superinterface.super.foo() 也属于调用形式的第六种情况TypeName.super. [TypeArguments] Identifier，SuperInterface是接口，这种形式需要满足一个基本条件，即代码所在的类或者包含类必须直接实现这个接口，间接实现不行。
这里Subclass2直接实现了Superinterface接口，满足条件，会使用Superinterface作为查找入口并匹配到它的default方法。<br/>
Note: 如果Superinterface没有匹配的default方法，会继续往它的父接口继续查找。

案例4：
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
按照上面的规则，第一步不执行装箱和拆箱操作进行匹配，“20”可以安全的转换为long类型，所以会匹配到“calc(long value)”这个方法，执行的时候会打印出“20 calc by calc(long value).”, 执行结果有点反直觉。

案例5：
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
把案例4修改下增加“calc(int value)”，"calc(20)" 在第一阶段可以匹配到“calc(long value)”和"calc(int value)", “20”是int类型，"calc(int value)"对它来说更精确，所以会匹配到它，执行的时候会输出“20 calc by calc(int value).”<br/>
"calc(Integer.valueOf(20))"则会匹配到“calc(Integer value)”, 执行的时候输出“20 calc by calc(Integer value).”。

案例6：
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
第一个调用“query(null)”的实参"null"是任何对象类型的空值，它可以被自动的转换成任何非原始数据类型，三个方法的参数类型分别为“Object”， “Object[]”和“String[]”，“null”都可以匹配到，并且“Object[]”比“Object”具体，“String[]”又比“Object[]”更具体，因此它最终会匹配到“query(String... value)”这个方法。<br/>
对象数组比对象更具体的原因是因为数组也是对象，数组可以赋值给对象变量，反过来不行，感兴趣可以查看这个[链接](https://docs.oracle.com/javase/specs/jls/se17/html/jls-10.html) 。<br/>
对于“query(Object) null)”调用，实参已经被强制转成“Object”类型，因而它的最会匹配到“query(Object value)”方法。

有兴趣的同学可以试着分析下下面的例子，然后运行代码对比下结果和自己分析的是否一致：
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

Java代码编译后，调用的方法以及方法签名就确定了，字节码会把这些信息都记录在class文件，执行阶段会依据这些信息确定运行时最终被执行的方法，比如成员方法调用要确定是否存子类覆盖(多态)的情况，我们留待下次分解。

# 参考文档
[Java语言规范之方法表达式](https://docs.oracle.com/javase/specs/jls/se18/html/jls-15.html#jls-15.12)