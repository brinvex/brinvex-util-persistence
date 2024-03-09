# Brinvex-Util-Persistence

## Introduction

_Brinvex-Util-Persistence_ is a compact Java library that adds utilities for Jakarta-JPA / Hibernate.


## Maven dependency declaration
- Compile dependencies intended for @Entity classes 
````
<dependency>
    <groupId>com.brinvex.util</groupId>
    <artifactId>brinvex-util-persistence-entity-api</artifactId>
    <version>1.5.3</version>
</dependency>
````
- Compile and runtime dependencies intended for DAO classes
````
<dependency>
    <groupId>com.brinvex.util</groupId>
    <artifactId>brinvex-util-persistence-api</artifactId>
    <version>1.5.3</version>
</dependency>
<dependency>
    <groupId>com.brinvex.util</groupId>
    <artifactId>brinvex-util-persistence-impl</artifactId>
    <version>1.5.3</version>
    <scope>runtime</scope>
</dependency>
````
- Standalone Java Utility for Postgresql Administration
````
<dependency>
    <groupId>com.brinvex.util</groupId>
    <artifactId>brinvex-util-persistence-dba-postgresql</artifactId>
    <version>1.5.3</version>
</dependency>
````

## Compatibility

| Brinvex-Util-Persistence | Java                                                       | Hibernate ORM | 
|--------------------------|------------------------------------------------------------|---------------|
| 1.4.*                    | **11**, 12, 13, 14, 15, 16, **17**, 18, 19, 20, **21**     | 6.2, 6.3      | 
| 1.5.*                    | **11**, 12, 13, 14, 15, 16, **17**, 18, 19, 20, **21**     | 6.2, 6.3, 6.4 | 

- **requires Hibernate ORM**
- can be used in Spring based applications
- can be used in Jakarta EE applications
- can be used in Java SE applications


### License

- The _Brinvex-Util-Persistence_ is released under version 2.0 of the Apache License.
