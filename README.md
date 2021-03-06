# ixa-pipe-ned-ukb

This repository contains the Named Entity Disambiguation tool based on
[UKB](http://ixa2.si.ehu.es/ukb/). *ixa-pipe-ned-ukb* is developed
by the [IXA NLP Group](http://ixa.si.ehu.es/Ixa).

The *ixa-pipe-ned-ukb* module takes a [NAF
document](http://wordpress.let.vupr.nl/naf/) containing *wf*, *term*
and *entity* elements as input, performs Named Entity Disambiguation
for your language of choice, and outputs a NAF document with
references to Wikipedia on *entities* elements.


## TABLE OF CONTENTS

1. [Overview of ixa-pipe-ned-ukb](#overview)
2. [Installation](#installation)
3. [Usage of ixa-pipe-ned-ukb](#usage)



## OVERVIEW


### Module contents

The contents of the module are the following:

    + scripts/	    	 perl scripts of the module
    + src/   	    	 java source code of the module
    + config.properties  configuration file
    + pom.xml 	    	 maven pom file wich deals with everything related to compilation and execution of the module
    + COPYING	    	 license file
    + README.md	    	 this README file
    + Furthermore, the installation process, as described in the README.md, will generate another directory:
    target/	         it contains binary executable and other directories




## INSTALLATION

Installing the *ixa-pipe-ned-ukb* requires the following steps:

*If you already have installed in your machine the Java 1.7+ and MAVEN
3, please go to [step 4](#4-download-and-install-ukb-and-its-resources)
directly. Otherwise, follow the detailed steps*

### 1. Install JDK 1.7 or JDK 1.8

If you do not install JDK 1.7+ in a default location, you will probably
need to configure the PATH in .bashrc or .bash_profile:

````shell
export JAVA_HOME=/yourpath/local/java17
export PATH=${JAVA_HOME}/bin:${PATH}
````

If you use tcsh you will need to specify it in your .login as follows:

````shell
setenv JAVA_HOME /usr/java/java17
setenv PATH ${JAVA_HOME}/bin:${PATH}
````

If you re-login into your shell and run the command

````shell
java -version
````

you should now see that your JDK is 1.7 or 1.8.


### 2. Install MAVEN 3

Download MAVEN 3 from

````shell
wget http://apache.rediris.es/maven/maven-3/3.0.5/binaries/apache-maven-3.0.5-bin.tar.gz
````

Now you need to configure the PATH. For Bash Shell:

````shell
export MAVEN_HOME=/yourpath/local/apache-maven-3.0.4
export PATH=${MAVEN_HOME}/bin:${PATH}
````

For tcsh shell:

````shell
setenv MAVEN3_HOME ~/local/apache-maven-3.0.4
setenv PATH ${MAVEN3}/bin:{PATH}
````

If you re-login into your shell and run the command

````shell
mvn -version
````

You should see reference to the MAVEN version you have just installed plus the JDK that is using.



### 3. Install Perl
Install Perl and the required packages.
For example, you might need to install the SQLite package as follows:
````shell
apt-get install libdbd-sqlite3-perl
````


### 4. Download and install UKB and its resources

Download [UKB](http://ixa2.si.ehu.es/ukb/) and unpack it:

````shell
wget http://ixa2.si.ehu.es/ukb/ukb_2.2.tgz
tar xzvf ukb_2.2.tgz
````

If you are using a x86-64 Linux platform, you can use the already
compiled *ukb_wsd* binary in *ukb/bin* folder. If not, follow the
installation instructions in *ukb/src/INSTALL* file.

Download and create a graph derived from Wikipedia.
For example, to get the Basque Wikipedia graph, first download the
following source files and unpack them:

````shell
wget http://ixa2.si.ehu.es/ukb/graphs/wikipedia_eu_2016.tar.bz2
bunzip2 wikipedia_eu_2016.tar.bz2
tar xvf wikipedia_eu_2016.tar
````

Next, create the Wikipedia graph following the compilation instructions in *ukb/src/README* file (section 1.2 Compiling the KB).


### 5. Download the SQLite database derived from Wikipedia

Download and unpack the required SQLite database:

   - Basque wikipedia: 

````shell
wget http://ixa2.si.ehu.es/ixa-pipes/models/2016Apr_wiki_eu.db.tgz
tar xzvf 2016Apr_wiki_eu.db.tgz
````


### 6. Get module source code

````shell
git clone https://github.com/ixa-ehu/ixa-pipe-ned-ukb
````

### 7. Compile

````shell
cd ixa-pipe-ned-ukb
mvn clean package
````

This step will create a directory called 'target' which contains
various directories and files. Most importantly, there you will find
the module executable:

````shell
ixa-pipe-ned-ukb-${version}.jar
````

This executable contains every dependency the module needs, so it is
completely portable as long as you have a JVM 1.7 installed.


## USAGE

The *ixa-pipe-ned-ukb* requires a NAF document (with *wf*, *term*
and *entity* elements) as standard input and outputs NAF through
standard output. You can get the necessary input for *ixa-pipe-ned-ukb*
by piping *[ixa-pipe-tok](https://github.com/ixa-ehu/ixa-pipe-tok)*,
*[ixa-pipe-pos](https://github.com/ixa-ehu/ixa-pipe-pos)* and
*[ixa-pipe-nerc](https://github.com/ixa-ehu/ixa-pipe-nerc)* as shown
in the example below.

First, configure the module updating the paths in the
*config.properties* configuration file.

It has a parameter:
+ **-c**: path to the configuration file.

You can call to *ixa-pipe-ned-ukb* module as follows:

````shell
cat text.txt | ixa-pipe-tok | ixa-pipe-pos | ixa-pipe-nerc | java -jar ixa-pipe-ned-ukb-${version}.jar -c config.properties
````

When the language is other than English, the module offers an
additional feature. It is possible to set the corresponding English
entry also. To use this option, first get the database created by
[MapDB](http://www.mapdb.org/) which has the crosslingual links.
So far, you can download and untar the following package for Basque
crosslingual links:

````shell
wget http://ixa2.si.ehu.es/ixa-pipes/models/2016Apr_wiki_eu2en-db.tgz
tar xzvf 2016Apr_wiki_eu2en-db.tgz
````

Then, specify the *CrossWikipediaIndex* parameter in the *config.properties* file.


For more options running *ixa-pipe-ned-ukb*:

````shell
java -jar ixa-pipe-ned-ukb-${version}.jar -h
````


#### Contact information

    Arantxa Otegi
    arantza.otegi@ehu.eus
    IXA NLP Group
    University of the Basque Country UPV/EHU
    E-20018 Donostia-San Sebastián


