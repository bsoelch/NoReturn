# NoReturn 
NoReturn is a programing language build around the idea, that 
procedures do not return to the position where they were called

!!! This language is currently in early development !!!

## Syntax
### Comments
Line comments start with `##`, 
block comments are surrounded by `#_` and `_#`

Example:
```
this = code; ## line comment
that #_block comment_# = code;
```

### Types
#### Atomic Types
There are 14 atomic types:
- uint8, uint16, uint32, uint64: unsigned integers
- int8, int16, int32, int64: signed integers
- float32, float64: floating point numbers
- string: a string of characters
- bool: a boolean value (true or false)
- type: a Type
- any: Wildcard Type that can contain any value
#### Containers
- arrays: (syntax: `<Type>'[]'`) contain a sequence of values of 
Type < Type > indexed with an uint64 
elements of an array are accessible though the [] operator
- optionals: (syntax: `<Type>'?'`) contains a value of Type < Type >
or none
- reference: (syntax `'@'<Type>`) contains a reference to a value 
of Type < Type >, can be used to share values 
between different procedures
- Struct: (syntax: `'{'<Type>':'<Name>
(','<Type>':'<Name )*'}'`) can be used to group
data, fields of a Struct can be accessed with .

Examples:
```
uint64[]
int32?
@float64
{int64:numerator,uint64:denominator}

{int32:arg1,@(int32?):arg2}[][]
```

!!! the handling of values in container types 
is currently only partially implement !!!

#### Procedures
Procedures are treated as normal values, 
types of procedures are defined though the syntax:
`'('<Type>(','<Type>)')=>?'`
#### Generics
A generic type is declared though $ followed by an 
identifier, generic allow declaration of dependent types 
in procedure signatures. At runtime generics are treated as
Type any.
Example:

```
($a,(string,$a)=>?)=>?
```
is the signature of a procedure that creates a string 
and then passes it to another procedure, while 
allowing the caller to pass an arbitrary addition argument
to that procedure

#### Typedef
With the keyword "typedef", it is possible to define 
name aliases for types. 
A typedef statement is typedef, followed by a type-name 
and ends with ;
Example:
```C
typedef int32 int;
typedef int64 long;
typedef float32 float;
typedef float64 double;
```
create type aliases for the primitive java types
int, long, float and double

### Constants
The "const" keyword can be used to define global constants.
Constant definitions have the syntax:
'const '< Type >':'< Name >'='< Expression >';'

Example:
```C
const float64:pi=3.14159265358979;
const float64:sq_pi=pi*pi;
```
### Procedures
Procedures are the main building block of no-ret programs.
Each procedure takes the given arguments, preforms 
the atomic actions in the procedure-body and the calls 
zero or more child-procedures
A procedure definition has the syntax:
```
<Name>'('<ArgTypes>'):('<ArgNames>') => {'
<Actions>'
} => ['<Calls>']'
```
- `<Name>`: A procedure name is any valid identifier that is
not already in use for a typedef, const or (native) procedure
- `<ArgTypes>`:The types for the arguments of the procedure
separated by ,
- `<ArgNames>`:The names for the procedure arguments
that will be used in its body
- `<Actions>`: A sequence of [atomic Actions](https://github.com/bsoelch/NoReturn#Actions) 
terminated by semicolons
- `<Calls>`: zero or more calls to child procedures
each call has the form `<ProcName>'('<Expr>(','<Expr>)*')'`
with `<ProcName>` being the name of called procedure
(local or global) variable and `<Expr>` being an 
[expression](https://github.com/bsoelch/NoReturn#Expressions)

### Actions
Currently, there are two types of actions:
#### Variable declarations:
Creates a new local variable with 
the given type and initial value.
syntax: `<Type>':'<Name>'='<Expr>';'`

Examples:
```
int32:x=16;
any : value = {1,2,3,{4,5,6}};
{int64:num,uint64:den} : frac={.num=2,.den=(uint64:)3};
```

#### Assignments
Assigns a new value to (a field/element of) a variable.
syntax: `<Expr>'='<Expr>';'`
The left-hand side supports local variables,
array-access, field access and typecasts, changes to value 
in one variable do not have effect on other variables
(!!! the current version has not yet implemented that behaviour!!!)

Examples:
```
x=3;
array[0]={1,2,3};
(({int32:val}[]:)y)[0].val=17;
```

### Expressions
#### Values
##### Numbers
Integers or floats in base 2,10 or 16.
Floating points numbers in base 2 and 16 are 
completely in that base, including the exponent part
(`0b1E-10` is 2<sup>-2</sup> and `0x1P10` is 16<sup>16</sup>)
Integer literals are interpreted as int32, if they don't fit into an int32 
they are interpreted as int64. 
Integers ending with `u` or `U` are interpreted as unsigned.
(uint32 or uint64). 
All float literals are interpreted as double by default.

Examples:
```
int32:i=1;
uint64:m=1234u;
float64[]:array={1.0,0x3.4P1a,0b11.01E-11};
int64:l=0x123456789abcdef;
int8:bin=(int8:)0b11001001;
```


##### Strings
String literals start and end with `"`,
`\ ` can be used for escaping.

```
string:s1="Hello World!";
string:escaped="\"Hello\" World!";
```

##### Characters
A character literal is a single unicode character
surrounded by `'`, `\ ` can be used for escaping.
Character literals evaluate to the unicode codepoint-id
of their character (as uint32)

```
uint32:c1='A';
uint32:c2='\n';
```

##### native Constants
The following Names are reserved for native constants:
- true: true boolean
- false: false boolean
- none: empty optional (can be cast to any optional type)
- NOP: no-op procedure (can be cast to any procedure type)
##### Arrays
Arrays are declared as a list of elements surround by
{}

Example:
```
float64[]:array={1.0,0x3.4P1a,0b11.01E-11};
```
##### Structs
The struct declaration syntax is like in C a list
of statements of the form `'.'<field>'='<Value>`
Example:
```
{int64:num,uint64:den}:frac={.num=2,.den=3u};
```

#### Variables
All identifiers that are not numbers, typedefs or fields accesses
are interpreted as variable names. Variables can be
local variables, constants or procedure names.

Example:
```C
const int32:constant=42;

procedure(int32):(a)=>{
    int32:ret=a*a-constant;
    res=ret<0?procedure:NOP;
}=>[res(ret)]
```
##### field access
Fields of variables can be accessed with .
- All values have an immutable field `type` which contains 
the type of that value.
- Arrays and Strings additionally have an immutable field `length`
containing their length as an unsigned 64-bit integer.
- Structs have a mutable field for each element

```
type:t=1.type;
uint64:len={1,2,3,4}.length;
{int64:num,uint64:den} fract={.num=22,.den=7u};
int64:num=frac.num;
frac.num=355;
frac.den=113;
```

##### index access
With the `[]` operator it is possible to access elements
of arrays and strings.
Strings are interpreted as `uint8[]` containing their 
utf8 representation

```
int32[]:array={1,2,3,4,5,6,7};
int32:frist=array[0];
string:s="Hello World";
uint8:char=s[6];
s[4]=0x20;
array[0]=0;
```

##### range access
Range access operations are currently not implemented

#### Operators
Binary Operators:
- `+` adds two numbers, concatenates strings or arrays
- `-`,`*` subtractions/product of two numbers
- `/` floating point division
- `//` integer division
- `%` remainder
- `**` power-operator
- `<<` `>>` bit-shift operators 
or push first/last if one operand is an array
- `&` `|` `^` bitwise logical operations
- `&&` `||` logical and/or
- `<=` `<` `>` `>=` comparison operators
(work for strings or numbers)
- `==` `!=` equals/not equals
Unary Operators:
- `+` `-` sign operators
- `!` logical not
- `~` flip all bits

Examples:
```
any[]:array={-1+2,3/4,5**6,7*8,9&10,11|12,13^14}
array=0>>(array<<15)
```
results in 
```
array={0,1,0.75,15625.0,56,8,15,3,15}
```

#### Typecasts
The syntax `(<Type>:)<Expr>` allows to cast an expression 
to a different type. It is possible to directly assign 
values of a type to another type if the assignment does 
not lead to a loss of data.

Examples:
```
uint8:byte=(uint8:)1234;
float64:aFloat=16;
any:value={"string",{1,2,3,{}}};
```


