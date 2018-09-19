package org.runestar.cs2;

public final class Opcodes {

    public static final int SS_AND = -2;

    public static final int SS_OR = -1;

    private Opcodes() {}

    public static final int PUSH_CONSTANT_INT = 0;

    public static final int PUSH_VAR = 1;

    public static final int POP_VAR = 2;

    public static final int PUSH_CONSTANT_STRING = 3;

    public static final int BRANCH = 6;

    public static final int BRANCH_NOT = 7;

    public static final int BRANCH_EQUALS = 8;

    public static final int BRANCH_LESS_THAN = 9;

    public static final int BRANCH_GREATER_THAN = 10;

    public static final int RETURN = 21;

    public static final int PUSH_VARBIT = 25;

    public static final int POP_VARBIT = 27;

    public static final int BRANCH_LESS_THAN_OR_EQUALS = 31;

    public static final int BRANCH_GREATER_THAN_OR_EQUALS = 32;

    public static final int PUSH_INT_LOCAL = 33;

    public static final int POP_INT_LOCAL = 34;

    public static final int PUSH_STRING_LOCAL = 35;

    public static final int POP_STRING_LOCAL = 36;

    public static final int JOIN_STRING = 37;

    public static final int POP_INT_DISCARD = 38;

    public static final int POP_STRING_DISCARD = 39;

    public static final int GOSUB_WITH_PARAMS = 40;

    public static final int _42 = 42;

    public static final int _43 = 43;

    public static final int DEFINE_ARRAY = 44;

    public static final int PUSH_ARRAY_INT = 45;

    public static final int POP_ARRAY_INT = 46;

    public static final int _47 = 47;

    public static final int _48 = 48;

    public static final int SWITCH = 60;

    public static final int CC_CREATE = 100;

    public static final int CC_DELETE = 101;

    public static final int CC_DELETEALL = 102;

    public static final int _200 = 200;

    public static final int _201 = 201;

    public static final int CC_SETPOSITION = 1000;

    public static final int CC_SETSIZE = 1001;

    public static final int CC_SETHIDE = 1003;

    public static final int _1005 = 1005;

    public static final int _1006 = 1006;

    public static final int CC_SETSCROLLPOS = 1100;

    public static final int CC_SETCOLOUR = 1101;

    public static final int CC_SETFILL = 1102;

    public static final int CC_SETTRANS = 1103;

    public static final int CC_SETLINEWID = 1104;

    public static final int CC_SETGRAPHIC = 1105;

    public static final int CC_SET2DANGLE = 1106;

    public static final int CC_SETTILING = 1107;

    public static final int CC_SETMODEL = 1108;

    public static final int CC_SETMODELANGLE = 1109;

    public static final int CC_SETMODELANIM = 1110;

    public static final int CC_SETMODELORTHOG = 1111;

    public static final int CC_SETTEXT = 1112;

    public static final int CC_SETTEXTFONT = 1113;

    public static final int CC_SETTEXTALIGN = 1114;

    public static final int CC_SETTEXTANTIMACRO = 1115;

    public static final int CC_SETOUTLINE = 1116;

    public static final int CC_SETGRAPHICSHADOW = 1117;

    public static final int CC_SETVFLIP = 1118;

    public static final int CC_SETHFLIP = 1119;

    public static final int CC_SETSCROLLSIZE = 1120;

    public static final int _1121 = 1121;

    public static final int _1122 = 1122;

    public static final int _1123 = 1123;

    public static final int _1124 = 1124;

    public static final int _1125 = 1125;

    public static final int _1126 = 1126;

    public static final int _1127 = 1127;

    public static final int CC_SETOBJECT = 1200;

    public static final int CC_SETNPCHEAD = 1201;

    public static final int CC_SETPLAYERHEAD_SELF = 1202;

    public static final int CC_SETOBJECT_NONUM = 1205;

    public static final int CC_SETOBJECT_ALWAYS_NUM = 1212;

    public static final int CC_SETOP = 1300;

    public static final int CC_SETDRAGGABLE = 1301;

    public static final int CC_SETDRAGGABLEBEHAVIOR = 1302;

    public static final int CC_SETDRAGDEADZONE = 1303;

    public static final int CC_SETDRAGDEADTIME = 1304;

    public static final int CC_SETOPBASE = 1305;

    public static final int CC_SETTARGETVERB = 1306;

    public static final int CC_CLEAROPS = 1307;

    public static final int CC_SETONCLICK = 1400;

    public static final int CC_SETONHOLD = 1401;

    public static final int CC_SETONRELEASE = 1402;

    public static final int CC_SETONMOUSEOVER = 1403;

    public static final int CC_SETONMOUSELEAVE = 1404;

    public static final int CC_SETONDRAG = 1405;

    public static final int CC_SETONTARGETLEAVE = 1406;

    public static final int CC_SETONVARTRANSMIT = 1407;

    public static final int CC_SETONTIME = 1408;

    public static final int CC_SETONTOP = 1409;

    public static final int CC_SETONDRAGCOMPLETE = 1410;

    public static final int CC_SETONCLICKREPEAT = 1411;

    public static final int CC_SETONMOUSEREPEAT = 1412;

    public static final int CC_SETONINVTRANSMIT = 1414;

    public static final int CC_SETONSTATTRANSMIT = 1415;

    public static final int CC_SETONTARGETENTER = 1416;

    public static final int CC_SETONSCROLLWHEEL = 1417;

    public static final int CC_SETONCHATTRANSMIT = 1418;

    public static final int CC_SETONKEY = 1419;

    public static final int _1420 = 1420;

    public static final int _1421 = 1421;

    public static final int _1422 = 1422;

    public static final int _1423 = 1423;

    public static final int _1424 = 1424;

    public static final int _1425 = 1425;

    public static final int _1426 = 1426;

    public static final int _1427 = 1427;

    public static final int CC_GETX = 1500;

    public static final int CC_GETY = 1501;

    public static final int CC_GETWIDTH = 1502;

    public static final int CC_GETHEIGHT = 1503;

    public static final int CC_GETHIDE = 1504;

    public static final int _1505 = 1505;

    public static final int CC_GETSCROLLX = 1600;

    public static final int CC_GETSCROLLY = 1601;

    public static final int CC_GETTEXT = 1602;

    public static final int CC_GETSCROLLWIDTH = 1603;

    public static final int CC_GETSCROLLHEIGHT = 1604;

    public static final int CC_GETMODELZOOM = 1605;

    public static final int CC_GETMODELANGLE_X = 1606;

    public static final int CC_GETMODELANGLE_Z = 1607;

    public static final int CC_GETMODELANGLE_Y = 1608;

    public static final int CC_GETTRANS = 1609;

    public static final int _1610 = 1610;

    public static final int _1611 = 1611;

    public static final int _1612 = 1612;

    public static final int _1613 = 1613;

    public static final int _1614 = 1614;

    public static final int CC_GETINVOBJECT = 1700;

    public static final int CC_GETINVCOUNT = 1701;

    public static final int CC_GETID = 1702;

    public static final int CC_GETTARGETMASK = 1800;

    public static final int CC_GETOP = 1801;

    public static final int CC_GETOPBASE = 1802;

    public static final int _1927 = 1927;

    public static final int IF_SETPOSITION = 2000;

    public static final int IF_SETSIZE = 2001;

    public static final int IF_SETHIDE = 2003;

    public static final int _2005 = 2005;

    public static final int _2006 = 2006;

    public static final int IF_SETSCROLLPOS = 2100;

    public static final int IF_SETCOLOUR = 2101;

    public static final int IF_SETFILL = 2102;

    public static final int IF_SETTRANS = 2103;

    public static final int IF_SETLINEWID = 2104;

    public static final int IF_SETGRAPHIC = 2105;

    public static final int IF_SET2DANGLE = 2106;

    public static final int IF_SETTILING = 2107;

    public static final int IF_SETMODEL = 2108;

    public static final int IF_SETMODELANGLE = 2109;

    public static final int IF_SETMODELANIM = 2110;

    public static final int IF_SETMODELORTHOG = 2111;

    public static final int IF_SETTEXT = 2112;

    public static final int IF_SETTEXTFONT = 2113;

    public static final int IF_SETTEXTALIGN = 2114;

    public static final int IF_SETTEXTANTIMACRO = 2115;

    public static final int IF_SETOUTLINE = 2116;

    public static final int IF_SETGRAPHICSHADOW = 2117;

    public static final int IF_SETVFLIP = 2118;

    public static final int IF_SETHFLIP = 2119;

    public static final int IF_SETSCROLLSIZE = 2120;

    public static final int _2121 = 2121;

    public static final int _2122 = 2122;

    public static final int _2123 = 2123;

    public static final int _2124 = 2124;

    public static final int _2125 = 2125;

    public static final int _2126 = 2126;

    public static final int _2127 = 2127;

    public static final int IF_SETOBJECT = 2200;

    public static final int IF_SETNPCHEAD = 2201;

    public static final int IF_SETPLAYERHEAD_SELF = 2202;

    public static final int IF_SETOBJECT_NONUM = 2205;

    public static final int IF_SETOBJECT_ALWAYS_NUM = 2212;

    public static final int IF_SETOP = 2300;

    public static final int IF_SETDRAGGABLE = 2301;

    public static final int IF_SETDRAGGABLEBEHAVIOR = 2302;

    public static final int IF_SETDRAGDEADZONE = 2303;

    public static final int IF_SETDRAGDEADTIME = 2304;

    public static final int IF_SETOPBASE = 2305;

    public static final int IF_SETTARGETVERB = 2306;

    public static final int IF_CLEAROPS = 2307;

    public static final int IF_SETONCLICK = 2400;

    public static final int IF_SETONHOLD = 2401;

    public static final int IF_SETONRELEASE = 2402;

    public static final int IF_SETONMOUSEOVER = 2403;

    public static final int IF_SETONMOUSELEAVE = 2404;

    public static final int IF_SETONDRAG = 2405;

    public static final int IF_SETONTARGETLEAVE = 2406;

    public static final int IF_SETONVARTRANSMIT = 2407;

    public static final int IF_SETONTIME = 2408;

    public static final int IF_SETONTOP = 2409;

    public static final int IF_SETONDRAGCOMPLETE = 2410;

    public static final int IF_SETONCLICKREPEAT = 2411;

    public static final int IF_SETONMOUSEREPEAT = 2412;

    public static final int IF_SETONINVTRANSMIT = 2414;

    public static final int IF_SETONSTATTRANSMIT = 2415;

    public static final int IF_SETONTARGETENTER = 2416;

    public static final int IF_SETONSCROLLWHEEL = 2417;

    public static final int IF_SETONCHATTRANSMIT = 2418;

    public static final int IF_SETONKEY = 2419;

    public static final int _2420 = 2420;

    public static final int _2421 = 2421;

    public static final int _2422 = 2422;

    public static final int _2423 = 2423;

    public static final int _2424 = 2424;

    public static final int _2425 = 2425;

    public static final int _2426 = 2426;

    public static final int _2427 = 2427;

    public static final int IF_GETX = 2500;

    public static final int IF_GETY = 2501;

    public static final int IF_GETWIDTH = 2502;

    public static final int IF_GETHEIGHT = 2503;

    public static final int IF_GETHIDE = 2504;

    public static final int _2505 = 2505;

    public static final int IF_GETSCROLLX = 2600;

    public static final int IF_GETSCROLLY = 2601;

    public static final int IF_GETTEXT = 2602;

    public static final int IF_GETSCROLLWIDTH = 2603;

    public static final int IF_GETSCROLLHEIGHT = 2604;

    public static final int IF_GETMODELZOOM = 2605;

    public static final int IF_GETMODELANGLE_X = 2606;

    public static final int IF_GETMODELANGLE_Z = 2607;

    public static final int IF_GETMODELANGLE_Y = 2608;

    public static final int IF_GETTRANS = 2609;

    public static final int _2610 = 2610;

    public static final int _2611 = 2611;

    public static final int _2612 = 2612;

    public static final int _2613 = 2613;

    public static final int _2614 = 2614;

    public static final int IF_GETINVOBJECT = 2700;

    public static final int IF_GETINVCOUNT = 2701;

    public static final int IF_GETID = 2702;

    public static final int _2706 = 2706;

    public static final int IF_GETTARGETMASK = 2800;

    public static final int IF_GETOP = 2801;

    public static final int IF_GETOPBASE = 2802;

    public static final int _2927 = 2927;

    public static final int _3100 = 3100;

    public static final int _3101 = 3101;

    public static final int _3103 = 3103;

    public static final int _3104 = 3104;

    public static final int _3105 = 3105;

    public static final int _3106 = 3106;

    public static final int _3107 = 3107;

    public static final int _3108 = 3108;

    public static final int _3109 = 3109;

    public static final int _3110 = 3110;

    public static final int _3111 = 3111;

    public static final int _3112 = 3112;

    public static final int _3113 = 3113;

    public static final int _3115 = 3115;

    public static final int _3116 = 3116;

    public static final int _3117 = 3117;

    public static final int _3118 = 3118;

    public static final int _3119 = 3119;

    public static final int _3120 = 3120;

    public static final int _3121 = 3121;

    public static final int _3122 = 3122;

    public static final int _3123 = 3123;

    public static final int _3124 = 3124;

    public static final int _3125 = 3125;

    public static final int _3126 = 3126;

    public static final int _3127 = 3127;

    public static final int _3128 = 3128;

    public static final int _3129 = 3129;

    public static final int _3130 = 3130;

    public static final int _3131 = 3131;

    public static final int _3132 = 3132;

    public static final int _3133 = 3133;

    public static final int _3134 = 3134;

    public static final int _3135 = 3135;

    public static final int _3136 = 3136;

    public static final int _3137 = 3137;

    public static final int _3138 = 3138;

    public static final int _3139 = 3139;

    public static final int _3200 = 3200;

    public static final int _3201 = 3201;

    public static final int _3202 = 3202;

    public static final int _3300 = 3300;

    public static final int _3301 = 3301;

    public static final int _3302 = 3302;

    public static final int _3303 = 3303;

    public static final int _3304 = 3304;

    public static final int _3305 = 3305;

    public static final int _3306 = 3306;

    public static final int _3307 = 3307;

    public static final int _3308 = 3308;

    public static final int _3309 = 3309;

    public static final int _3310 = 3310;

    public static final int _3311 = 3311;

    public static final int _3312 = 3312;

    public static final int _3313 = 3313;

    public static final int _3314 = 3314;

    public static final int _3315 = 3315;

    public static final int _3316 = 3316;

    public static final int _3317 = 3317;

    public static final int _3318 = 3318;

    public static final int _3321 = 3321;

    public static final int _3322 = 3322;

    public static final int _3323 = 3323;

    public static final int _3324 = 3324;

    public static final int _3325 = 3325;

    public static final int _3400 = 3400;

    public static final int ENUM = 3408;

    public static final int _3411 = 3411;

    public static final int _3600 = 3600;

    public static final int _3601 = 3601;

    public static final int _3602 = 3602;

    public static final int _3603 = 3603;

    public static final int _3604 = 3604;

    public static final int _3605 = 3605;

    public static final int _3606 = 3606;

    public static final int _3607 = 3607;

    public static final int _3608 = 3608;

    public static final int _3609 = 3609;

    public static final int _3611 = 3611;

    public static final int _3612 = 3612;

    public static final int _3613 = 3613;

    public static final int _3614 = 3614;

    public static final int _3615 = 3615;

    public static final int _3616 = 3616;

    public static final int _3617 = 3617;

    public static final int _3618 = 3618;

    public static final int _3619 = 3619;

    public static final int _3620 = 3620;

    public static final int _3621 = 3621;

    public static final int _3622 = 3622;

    public static final int _3623 = 3623;

    public static final int _3624 = 3624;

    public static final int _3625 = 3625;

    public static final int _3626 = 3626;

    public static final int _3627 = 3627;

    public static final int _3628 = 3628;

    public static final int _3629 = 3629;

    public static final int _3630 = 3630;

    public static final int _3631 = 3631;

    public static final int _3632 = 3632;

    public static final int _3633 = 3633;

    public static final int _3634 = 3634;

    public static final int _3635 = 3635;

    public static final int _3636 = 3636;

    public static final int _3637 = 3637;

    public static final int _3638 = 3638;

    public static final int _3639 = 3639;

    public static final int _3640 = 3640;

    public static final int _3641 = 3641;

    public static final int _3642 = 3642;

    public static final int _3643 = 3643;

    public static final int _3644 = 3644;

    public static final int _3645 = 3645;

    public static final int _3646 = 3646;

    public static final int _3647 = 3647;

    public static final int _3648 = 3648;

    public static final int _3649 = 3649;

    public static final int _3650 = 3650;

    public static final int _3651 = 3651;

    public static final int _3652 = 3652;

    public static final int _3653 = 3653;

    public static final int _3654 = 3654;

    public static final int _3655 = 3655;

    public static final int _3656 = 3656;

    public static final int _3657 = 3657;

    public static final int ADD = 4000;

    public static final int SUB = 4001;

    public static final int MULTIPLY = 4002;

    public static final int DIV = 4003;

    public static final int RANDOM = 4004;

    public static final int RANDOMINC = 4005;

    public static final int INTERPOLATE = 4006;

    public static final int ADDPERCENT = 4007;

    public static final int SETBIT = 4008;

    public static final int CLEARBIT = 4009;

    public static final int TESTBIT = 4010;

    public static final int MOD = 4011;

    public static final int POW = 4012;

    public static final int INVPOW = 4013;

    public static final int AND = 4014;

    public static final int OR = 4015;

    public static final int SCALE = 4018;

    public static final int APPEND_NUM = 4100;

    public static final int APPEND = 4101;

    public static final int APPEND_SIGNUM = 4102;

    public static final int LOWERCASE = 4103;

    public static final int FROMDATE = 4104;

    public static final int TEXT_GENDER = 4105;

    public static final int TOSTRING = 4106;

    public static final int COMPARE = 4107;

    public static final int PARAHEIGHT = 4108;

    public static final int PARAWIDTH = 4109;

    public static final int TEXT_SWITCH = 4110;

    public static final int ESCAPE = 4111;

    public static final int APPEND_CHAR = 4112;

    public static final int CHAR_ISPRINTABLE = 4113;

    public static final int CHAR_ISALPHANUMERIC = 4114;

    public static final int CHAR_ISALPHA = 4115;

    public static final int CHAR_ISNUMERIC = 4116;

    public static final int STRING_LENGTH = 4117;

    public static final int SUBSTRING = 4118;

    public static final int REMOVETAGS = 4119;

    public static final int STRING_INDEXOF_CHAR = 4120;

    public static final int STRING_INDEXOF_STRING = 4121;

    public static final int OC_NAME = 4200;

    public static final int OC_OP = 4201;

    public static final int OC_IOP = 4202;

    public static final int OC_COST = 4203;

    public static final int OC_STACKABLE = 4204;

    public static final int OC_CERT = 4205;

    public static final int OC_UNCERT = 4206;

    public static final int _4207 = 4207;

    public static final int _4208 = 4208;

    public static final int _4209 = 4209;

    public static final int _4210 = 4210;

    public static final int _4211 = 4211;

    public static final int _4212 = 4212;

    public static final int _5000 = 5000;

    public static final int _5001 = 5001;

    public static final int _5002 = 5002;

    public static final int _5003 = 5003;

    public static final int _5004 = 5004;

    public static final int _5005 = 5005;

    public static final int _5008 = 5008;

    public static final int _5009 = 5009;

    public static final int _5015 = 5015;

    public static final int _5016 = 5016;

    public static final int _5017 = 5017;

    public static final int _5018 = 5018;

    public static final int _5019 = 5019;

    public static final int _5020 = 5020;

    public static final int _5021 = 5021;

    public static final int _5022 = 5022;

    public static final int _5306 = 5306;

    public static final int _5307 = 5307;

    public static final int _5308 = 5308;

    public static final int _5309 = 5309;

    public static final int _5504 = 5504;

    public static final int _5505 = 5505;

    public static final int _5506 = 5506;

    public static final int _5530 = 5530;

    public static final int _5531 = 5531;

    public static final int _5630 = 5630;

    public static final int _6200 = 6200;

    public static final int _6201 = 6201;

    public static final int _6202 = 6202;

    public static final int _6203 = 6203;

    public static final int _6204 = 6204;

    public static final int _6205 = 6205;

    public static final int _6500 = 6500;

    public static final int _6501 = 6501;

    public static final int _6502 = 6502;

    public static final int _6506 = 6506;

    public static final int _6507 = 6507;

    public static final int _6511 = 6511;

    public static final int _6512 = 6512;

    public static final int _6513 = 6513;

    public static final int _6514 = 6514;

    public static final int _6515 = 6515;

    public static final int _6516 = 6516;

    public static final int _6518 = 6518;

    public static final int _6519 = 6519;

    public static final int _6520 = 6520;

    public static final int _6521 = 6521;

    public static final int _6522 = 6522;

    public static final int _6523 = 6523;

    public static final int _6524 = 6524;

    public static final int _6525 = 6525;

    public static final int _6526 = 6526;

    public static final int _6600 = 6600;

    public static final int _6601 = 6601;

    public static final int _6602 = 6602;

    public static final int _6603 = 6603;

    public static final int _6604 = 6604;

    public static final int _6605 = 6605;

    public static final int _6606 = 6606;

    public static final int _6607 = 6607;

    public static final int _6608 = 6608;

    public static final int _6609 = 6609;

    public static final int _6610 = 6610;

    public static final int _6611 = 6611;

    public static final int _6612 = 6612;

    public static final int _6613 = 6613;

    public static final int _6614 = 6614;

    public static final int _6615 = 6615;

    public static final int _6616 = 6616;

    public static final int _6617 = 6617;

    public static final int _6618 = 6618;

    public static final int _6619 = 6619;

    public static final int _6620 = 6620;

    public static final int _6621 = 6621;

    public static final int _6622 = 6622;

    public static final int _6623 = 6623;

    public static final int _6624 = 6624;

    public static final int _6625 = 6625;

    public static final int _6626 = 6626;

    public static final int _6627 = 6627;

    public static final int _6628 = 6628;

    public static final int _6629 = 6629;

    public static final int _6630 = 6630;

    public static final int _6631 = 6631;

    public static final int _6632 = 6632;

    public static final int _6633 = 6633;

    public static final int _6634 = 6634;

    public static final int _6635 = 6635;

    public static final int _6636 = 6636;

    public static final int _6637 = 6637;

    public static final int _6638 = 6638;

    public static final int _6639 = 6639;

    public static final int _6640 = 6640;

    public static final int _6693 = 6693;

    public static final int _6694 = 6694;

    public static final int _6695 = 6695;

    public static final int _6696 = 6696;

    public static final int _6697 = 6697;

    public static final int _6698 = 6698;

    public static final int _6699 = 6699;
}
