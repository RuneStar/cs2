# cs2 [![Discord](https://img.shields.io/discord/384870460640329728.svg?logo=discord)](https://discord.gg/G2kxrnU)

[**View scripts**](https://github.com/RuneStar/cs2-scripts)

ClientScript2 (cs2) is a client-side scripting language used in Old School RuneScape to dynamically update interfaces.
Players load scripts from the game cache in a custom bytecode format to be interpreted by the client.

#### Model

There are two main kinds of scripts:

ClientScript: Added as callbacks for actions such as user interactions with interface components.
When the user performs the action, such as click or mouse-leave, the script is executed.
Cannot return any values.

Procedure: can only be called inside of another script.
The caller may be of either kind.
Able to both take arguments and return values.

#### Signature

The first line of a script is its signature.
It contains the script's kind, name, the types and names of its parameters, and its return types.
A script may return multiple values.
A Procedure is indicated by `proc` and a ClientScript by `clientscript`.

`[proc,min](int $a, int $b)(int)`

`[clientscript,settrans](component $component, int $trans)`

Each script can take a maximum of 50 arguments and return a maximum of 50 values.
If a script does not return anything, the parentheses around its return types can be omitted.

#### Variables

Global variables:
Defined outside of scripts in config files.
Can be used and modified inside any script.
Accessed by prefixing its name with a percent sign, like `%raids_lobby_partysize`.
Internally stored as either varps, varbits, or varcs.

Global constants:
Unable to be modified.
Defined outside of scripts.
Accessed by prefixing its name with a caret, like `^iftype_rectangle`.

Local variables:
Must be defined and initialized to a valid value before being used.
Only accessible inside the script in which it is defined.
Accessed by prefixing its name with a dollar sign, like `$scrollbar`.
Arguments are also local variables which are initialized to the passed in values.

#### Types

`null` is a valid value for all types except for `string`

* `int` : a 32-bit signed integer. Written normally or as a hexadecimal literal. `null` is equivalent to `-1`
    * `5`, `-10`, `0x7f7f7f`
* `string` : a sequence of characters, can be empty.
Written enclosed in quotes.
String interpolation can be performed by enclosing a `string` typed expression in angle brackets and
embedding it in a `string` literal.
    * `"Show"`, `"Magic level: <tostring($lvl)>"`
* `boolean` :
    * `true`, `false`
* `coord` : a tile. Written as 5 numbers separated by underscores:
the plane, the coordinates of the map-square, then the coordinates inside of the map-square
    * `0_1_2_3_4`
* `component` an interface component. Written as the interface, a colon, then the component
    * `poh_jewellery_box:frame`
* `obj` : an item
* `namedobj` : an item explicitly specified in the code. Can be used as an `obj`
    * `lawrune`
* `enum`
* `stat` : a skill.
    * `attack`, `smithing`
* `graphic` : a sprite. Written with its name enclosed in quotes
    * `"magicoff,12"`
* `inv` : an inventory of items such as the player's inventory or the bank
* `model`
* `category`
* `loc`
* `area`
* `maparea`
* `fontmetrics` : a font
    * `p11_full`
* `char`
* `struct`
* `synth`
* `mapelement`
* `npc`
* `seq`

#### Arrays

Are a fixed length and all elements must be the same type.
Can be of any type except for `string`.
Max length of 5000.
`int` elements are initialized to `0`, all other types to `null`.
Max of 5 arrays per script.
Scripts may take only 1 array argument.
Arrays can only be local variables and never globals.

Defining `$array` as an `int` array of length `$len` : `def_int $array($len);`

Getting the `$i`th element of `$array` : `$array($i)`

Setting the `$i`th element of `$array` to `$n` : `$array($i) = $n;` 

#### Control Flow

The only things which can be used in the test condition of an `if` or `while` statement are the following operators:

* Equality comparisons
    * `=` : equals
    * `!` : not equals
* Relational comparisons
    * `<` `>` `<=` `>=` 
* Short circuiting logical operators
    * `&` : AND
    * `|` : OR

Combinations of these operators may also be used. 
Variables or other expressions of type `boolean` may not be used on their own or as arguments to the logical operators.
These operators cannot be used outside of an `if` or `while` statement.

Using an `if` statement to clamp an `int` value between `$max` and `$min`

```
if ($n > $max) {
    return($max);
} else if ($n < $min) {
    return($min);
} else {
    return($n);
}
```

Using a `while` statement to repeat something 10 times

```
def_int $i = 0;
while ($i < 10) {
    // ...
    $i = calc($i + 1);
}
```

There is no continue or break functionality.

Switch

Any expression that is not of type `string` can be the target of a `switch` statement.
The cases must be constants.
Multiple cases may lead to the same block.
There is no fall-through between blocks.
An optional `default` case can be used if none of the other cases are matched.

#### Comments

`//` Indicates the start of a line comment and all following text up until the end of the line is ignored

#### Instructions

Most instructions are indicated by their name followed by a list of their arguments, `sound_synth(2266, 1, 0)`.
If it takes no arguments the parentheses can be omitted, `clan_leavechat`.

A Procedure is called by using a tilde followed by its name and arguments, `~min($a, $b)`.
If it takes no arguments the parentheses can be omitted, `~wilderness_level`.

Math on `int` expressions is performed using the `calc` instruction.
It supports the infix operators `+` (addition), `-` (subtraction), `/` (division), `*` (multiplication), `%` (modulus),
`|` (bitwise OR), `&` (bitwise AND).
Precedence can be established using parentheses.
The entire mathematical operation is passed as an argument to `calc` and the result is returned.
`def_int $c = calc($a * $b + 2);`

An instruction which returns multiple values can be used either directly as multiple arguments to another instruction or
assigned to multiple variables, `$int2, $int3 = ~poh_jewellerybox_getbuttonspacing(590:2, 3);`

#### Hooks

A ClientScript can be assigned as a reoccurring callback for an action by using one of the instructions which start with either `if_seton` or `cc_seton`.
They take as their first argument the name of the ClientScript and its arguments all enclosed in quotes.
The ClientScript is not executed immediately but instead executed with the given parameters each time the action occurs.

`cc_setonmouseover("autocast_tooltip($component1, $obj5)");`

Instead of using existing variables as arguments, special values starting with `event_` can be used which are resolved at the time the action occurs.
If a mouse event is being used `event_mousex` will be replaced by the mouse's x position when the event is triggered.

`cc_setondrag("scrollbar_vertical_drag($component0, $component1, event_mousey, false)");`

ClientScripts can not only be assigned to interface actions but also to changes in varps, skills, or inventories.
In this case the values which should be watched are added in braces after the arguments.

`if_setonvartransmit("deadman_tournament_hudupdate(){var1542, var1293}", 90:11);`