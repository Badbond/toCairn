# toCairn

Proof of Concept for the thesis 'Identifying microservice boundaries in a monolithic application based on data
autonomy'. toCairn can cluster the classes based on multiple metrics. The metrics used determine the input required.
Multiple clustering algorithms can be used to identify the microservice boundaries. These algorithms include an
agglomerative hierarchical clustering algorithm as well as multiple multi-objective evolutionary clustering algorithms
(MOEA). As this is a Proof of Concept, please consider it for evaluation but not for production purposes.

This thesis project was conducted at the University of Amsterdam by Pieter Dirk Soels.

## Features

- Creates a dependency graph of your application.
- Identifies frequency of class interaction based on static source code analysis.
- Identifies frequency of class interaction based on a custom JaCoCo coverage report.
- Identifies size of a class based on JFR memory allocation analysis.
- Identify microservices based on multiple established metrics formed from microservice characteristics.
- Perform clustering using an agglomerative hierarchical clustering algorithm with provided weights for the metrics.
- Perform clustering using MOEA to generate a non-dominated solution space optimizing these multiple objectives.

## Running the project locally

Currently, the project is only built to be running locally. The project requires input files to be present on an
available filesystem path.

To run the project locally, you must have an instance of Neo4j running, the graph database that this project uses. For
convenience, a `docker-compose.yml` definition is provided in the project root to run an instance already configured to
run with this project. When you have `docker-compose` installed on your machine, you can boot up the Neo4j instance
using `docker-compose up -d`. If you run your own instance, please configure the server application to use your own
configured Neo4j instance through application properties (see `application.yml`).

To run the server, start the Spring server defined in `ThesisApplication.java`. This will start the server upon which it
will be accessible at `localhost:8080`.

## Prerequisites

For this application to work, you need to compile this project. For this, one needs to have Java JDK 11 and Maven 3.8+
installed. Furthermore, one needs to have a Neo4J database. One can set one up themselves manually or use our convenient
`docker-compose.yaml` script to set one up with defaults matching the application. Storage is persisted in a Docker
volume in this set up. For the latter, one needs to have both `docker` and `docker-compose` installed on their system.

Lastly, a convenience would be the use of Postman to communicate with our API. In the root of this repository, there is
a collection file (``) that you can import to start using the application straight away.

## Usage

This PoC does not come with a frontend. Instead, we use manual API requests to store and retrieve data as well as to
perform actions. On top of that, we use the visualisations from Neo4J to inspect our created graph and its configuration
manually.

Make sure you have a Neo4J database running either through the aforementioned manual set up or through running
`docker-compose up -d` in the root of this repository. Then, run `ToCairnApplication` to start the application.

You can then interact with the application on `http://localhost:8080` and interact with the database on
`localhost:7474`.

## FAQ

These are common questions and problems encountered and how to solve them.

### Neo4j missing annotations

When receiving an error message containing `Missing @TargetNode declaration in class`
or `missing a property for the generated, internal ID (@Id @GeneratedValue Long id)`, it could be that the project did
not compile correctly. Try to run it again or run `mvn clean compile` to recompile the project, and the problem should
be gone. This happens when initializing the Spring context both during running the application and during tests.

### Persistence queries seem to take very long

Due to how Spring Data Neo4J (SDN) is implemented it could be that, when persisting certain resources, a lot of queries
need to be executed. SDN tries to make sure that the graph database exactly matches the Java model. As most of our
resources are linked, this means that it will try to verify the whole graph every time you persist a resource. We tried
to mitigate this as much as possible with custom queries for persisting relationships as well as using projections for
storing and retrieving data.

However, it could be the case on a different model that it keeps getting stuck when persisting entities. To debug such
case for yourself, run the application with the `-Dlogging.level.org.springframework.data.neo4j=DEBUG` environment
variable set. This will log all the queries made to Neo4J in the console.

### Missing expected relationships

As the relationships between classes is determined using static code analysis, there are various reasons why certain
expected relationships are not present in the constructed graph. Most understandably at first, relationships based on
reflection that are only present during runtime are not possible (at least to a certain point) using static analysis.
The same goes for polymorphism. We can hardly deduce from static analysis whether a class is a certain subtype and
therefore we do not see such relationships.

Lastly, from static code analysis, we often only know the name of a class. However, to accurately match that class, we
need its fully qualified name. For that, we use JavaParser's symbol solver. From traffic on discussion fora, it seems
that this is still the most used AST parser and way to deduce this in meta-programming. However, this symbol solver is
(at the time of writing) unmaintained. Therefore, some AST could not be resolved due to, for example, new Java
constructs that the solver does not yet support (such as Java 10's `var`). Lastly, not providing dependency `.jar` files
greatly lowers the ability to solve class FQNs as often a type must be deduced from library usage (e.g. deserialization)
. To see such problems, you can enable debug logging through the environment property `logging=debug` which will print
them to the console.

## License

This project is licensed under the [Apache 2.0 license](LICENSE.txt).
