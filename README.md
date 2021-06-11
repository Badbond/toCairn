# Thesis Project

Proof of Concept on identifying microservice boundaries in a monolithic application using data autonomy, amongst others,
as objectives. For clustering the classes representing a microservice, multi-objective evolutionary clustering
algorithms are used. As this is a Proof of Concept, please consider it for evaluation but not for production purposes.

This thesis project was conducted at the University of Amsterdam by Pieter Dirk Soels.

## Features

_Coming soon_

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

## Usage

_Coming soon_

## FAQ

These are common questions and problems encountered and how to solve them.

### Neo4j missing annotations

When receiving an error message containing `Missing @TargetNode declaration in class`
or `missing a property for the generated, internal ID (@Id @GeneratedValue Long id)`, it could be that the project did
not compile correctly. Run `mvn clean compile` to recompile the project, and the problem should be gone. This happens
when initializing the Spring context both during running the application and during tests.

### NoClassDefFoundError during static analysis

_Explanation coming soon_

## License

This project is licensed under the [Apache 2.0 license](LICENSE.txt).