# Wrapper使用教程
-------------------
Zero Point Launcher秽土转生

通过修改Forge的加载机制来让Forge能识别不同地方的模组并正确加载。
支持的加载方式：1. 主模组文件夹；2. 指定额外模组文件夹；3. Gradle仓库格式模组文件；4. 任意位置模组文件（待更新）。

支持MultiMC系列启动器（Prism Launcher等）、Hello Minecraft Launcher等。暂不支持Plain Craft Launcher II

### 请仔细阅读

### 【使用方法】
添加java虚拟机jvm参数：-javaagent:<wrapper.jar>

<wrapper.jar>表示wrapper.jar的地址，相对（相当于与.minecraft运行时目录）或者绝对路径

例如：wrapper.jar在 C:\wrapper.jar。-javaagent:C:\wrapper.jar

例如：wrapper.jar在.minecraft。-javaagent:wrapper.jar

建议将wrapper.jar存放到.minecraft目录。

-javaagent:wrapper-0.4-beta-2.jar

### 【配置文件】
读取于.minecraft文件夹下的wrapper_config.json文件，为json格式

***active***：当前活跃的配置项，游戏下次启动将以对应的配置策略执行。

***settings***：所有的配置项，用户可以创建多份暂存

配置项详情：以此为例

***GTNH2.6.1***：本配置项的名称，通过修改active的值来使用此配置项目

***config***：不建议修改，只能重定向部分配置文件（计划重定向所有）

***main_mods***：主模组目录，部分模组的资源文件会解压到此（例如：ic2文件夹）

***extra_mods***: 额外模组目录，是个列表，不限制数量

***modsListFile***: gradle仓库显示的模组清单文件。形式特殊，建议使用TidyMods.py脚本.生成。不使用请留空或者删除该条。

### 【问题模组】
目前已知必须放在mods文件夹的模组：

所有模组均不必放置于主模组目录

目前已知必须放在主模组的模组：

（1）Healer: 用于修复log4j2漏洞

### 【建议使用策略】
mods #存放官方包的模组

extramods #存放私货模组

在mods文件夹下存放官方包的模组

在extramods文件夹下存放私货模组

### 【TidyMods使用教程】
1.  安装python3.11及以上版本
2.  将你对应版本的json文件与TidyMods.py放置于mods文件夹。
3.  执行python TidyMods.py等待片刻
4.  脚本将自动生成一个以Forge为前缀的json文件，该文件即为可以被识别的模组清单文件
5.  将该文件移动到.minecraft文件夹内
6.  在wrapper_config.json文件中对应的配置项内填写该文件名（记得带上.json）

对应版本号的json文件在仓库：https://github.com/wohaopa/Wrapper/tree/update/release 内可以找到。本仓库为触发式手动更新，若没有对应版本的json，可以提交一个Issue，Issue带有Release标签，正文内容为GTNH发布的清单文件的下载链接，一般位于 https://github.com/GTNewHorizons/DreamAssemblerXXL/tree/master/releases/manifests 这将会自动产生一个PR与一个新的分支。
