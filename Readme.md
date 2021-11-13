#NoReturn 
NoReturn is a programing language build around the idea, that 
procedures do not return to the position where they were called

!!! This language is currently in early development !!!

##Syntax
###Types
####Atomic Types
There are 14 atomic types:
- uint8, uint16, uint32, uint64: unsigned integers
- int8, int16, int32, int64: signed integers
- float32, float64: floating point numbers
- string: a string of characters
- bool: a boolean value (true or false)
- type: a Type
- any: Wildcard Type that can contain any value
####Containers
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

####Procedures
Procedures are treated as normal values, 
types of procedures are defined though the syntax:
`'('<Type>(','<Type>)')=>?'`
####Generics
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

####Typedef
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

##Constants
The "const" keyword can be used to define global constants.
Constant definitions have the syntax:
'const '< Type >':'< Name >'='< Expression >';'

Example:
```C
const float64:pi=3.14159265358979;
const float64:sq_pi=pi*pi;
```
##Procedures
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
##Actions
!!TODO!!
##Expressions
!!TODO!!

