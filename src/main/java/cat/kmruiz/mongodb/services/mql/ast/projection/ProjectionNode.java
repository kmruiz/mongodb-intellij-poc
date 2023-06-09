package cat.kmruiz.mongodb.services.mql.ast.projection;

public record ProjectionNode<Origin>(Origin origin, ProjectionComputationNode<Origin> children) {
}
