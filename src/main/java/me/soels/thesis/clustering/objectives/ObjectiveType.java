package me.soels.thesis.clustering.objectives;

public enum ObjectiveType {
    /**
     * The microservice should do only one thing and do it well.
     */
    ONE_PURPOSE, // TODO: Or smart endpoints, dumb pipelines: https://martinfowler.com/articles/microservices.html

    /**
     * The microservice is organized around a business capability or area.
     */
    BOUNDED_CONTEXT,

    /**
     * The microservice is responsible for managing and governing its own data.
     */
    DATA_AUTONOMY,

    /**
     * TODO See "You want to keep things that change at the same time in the same module" https://martinfowler.com/articles/microservices.html
     */
    SHARED_DEVELOPMENT_LIFECYCLE
}
