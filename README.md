# Brinvex-Util-Persistence

## Introduction

_Brinvex-Util-Persistence_ is a compact Java library built on top of _JPA/Hibernate_.
Its aim isn't to be as magical and versatile as _Spring Data_,
but rather to be straightforward and focused on _PostgreSQL_ and _Microsoft SQL Server_.


## Maven dependency declaration
- Compile and runtime dependencies intended for DAO classes
````
<dependency>
    <groupId>com.brinvex.util</groupId>
    <artifactId>brinvex-util-persistence-api</artifactId>
    <version>2.1.1</version>
</dependency>
<dependency>
    <groupId>com.brinvex.util</groupId>
    <artifactId>brinvex-util-persistence-impl</artifactId>
    <version>2.1.1</version>
    <scope>runtime</scope>
</dependency>
````
- Standalone Java Utility for PostgreSQL Administration
````
<dependency>
    <groupId>com.brinvex.util</groupId>
    <artifactId>brinvex-util-persistence-dba-postgresql</artifactId>
    <version>2.1.1</version>
</dependency>
````

## Compatibility

| Brinvex-Util-Persistence | Java                                                       | Hibernate ORM | 
|--------------------------|------------------------------------------------------------|---------------|
| 2.1.*                    | **11**, 12, 13, 14, 15, 16, **17**, 18, 19, 20, **21**, 22 | 6.5           | 
| 2.0.*                    | **11**, 12, 13, 14, 15, 16, **17**, 18, 19, 20, **21**, 22 | 6.4           | 
| 1.5.*                    | **11**, 12, 13, 14, 15, 16, **17**, 18, 19, 20, **21**, 22 | 6.2, 6.3, 6.4 | 

- **requires Hibernate ORM**
- can be used in Spring based applications
- can be used in Jakarta EE applications
- can be used in Java SE applications


### License

- The _Brinvex-Util-Persistence_ is released under version 2.0 of the Apache License.
