# RealTimeGame_improve
对RTS游戏进行改进，提升其胜率。

# how to understand AI in RTS
RTS 即时策略游戏，我们常说为RTS游戏做一个AI，不太像是人工智能，而是一个Algorithm Instance，即算法实例。怎么理解呢？就是我们根据已经有的底层API，忽略RTS游戏中每个Unit上的具体动作实现和显示在屏幕上的过程，而**把目光放到游戏策略、调度、对战过程本身上来**。

UnitType intro
--------

   |----|ProduceTime|MoveTime|Cost|Hp|AttackRange|Damage|SightRaidus|
|---|----|--------|-------|-------|-----------|------|-----------|
|Base|250||10|10||||5|
|Barracks|200||5|4|||3|

 |----|ProduceTime|MoveTime|Cost|Hp|AttackRange|Damage|SightRaidus|
|---|----|--------|-------|-------|-----------|------|-----------|
|Worker|50|10|1|1|1|1|3|
|Light |80|8 |2|4|1|2|2|
|Heavy |120|12|4|2|1|4|2|
|Ranged|100|10|2|1|3|1|3|

# The Art of War 《孙子兵法》
> 兵法的基本原则有五条：一是“度”，二是“量”，三是“数”，四是“称”，五是“胜”。敌我所处地域的不同，产生双方土地面积大小不同的“度”；敌我土地面积大小的“度”的不同，产生双方物产资源多少不同的“量”；敌我物产资源多少的“量”的不同，产生双方兵员多寡不同的“数”；敌我兵员多寡的“数”的不同，产生双方军事实力强弱不同的“称”；敌我军事实力强弱的“称”的不同，最终决定战争的胜负成败。胜利的军队较之于失败的军队，犹如以“镒”称“铢”那样占有绝对优势；而失败的军队较之于胜利的军队，就象用“铢”称“镒”那样处于绝对的劣势。军事实力强大的胜利者指挥部队作战，就象在万丈悬崖决开积水一样，一泻千里，所向披靡，这就是军事实力的“形”。




