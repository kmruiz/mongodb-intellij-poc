package cat.kmruiz.mongodb.services.mql.reporting;

public record QueryWarning<Source>(Source on, String message) {

}