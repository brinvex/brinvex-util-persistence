set JAVA_HOME="C:\tools\java\jdk-21.0.1"
set MVN=C:\tools\mvn\mvn-3.8.7\bin\mvn

REM Merge feature branch into master
REM Update version in README
REM Commit (dont push)

REM call %MVN% clean package

call %MVN% -P release clean license:format

REM Amend commit if there are some changes

call %MVN% -P release -Darguments=-DskipTests release:clean release:prepare

git push --follow-tags

call %MVN% -P release -Darguments=-DskipTests release:perform

echo Continue on https://s01.oss.sonatype.org/
