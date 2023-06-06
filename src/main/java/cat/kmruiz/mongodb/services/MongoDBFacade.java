package cat.kmruiz.mongodb.services;

import cat.kmruiz.mongodb.services.config.MongoDBConnectionConfiguration;
import cat.kmruiz.mongodb.services.mql.MQLIndex;
import cat.kmruiz.mongodb.services.mql.MongoDBNamespace;
import cat.kmruiz.mongodb.services.schema.CollectionSchema;
import com.intellij.codeInsight.daemon.impl.EditorTracker;
import com.intellij.database.psi.DbElement;
import com.intellij.database.util.DbUIUtil;
import com.intellij.database.vfs.DbVFSUtils;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiManager;
import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;

import java.util.*;

@Service(Service.Level.PROJECT)
public final class MongoDBFacade {

    private MongoDBConnectionConfiguration configuration;

    public record ConnectionAwareResult<T>(T result, boolean connected) {
        public static <T> ConnectionAwareResult<T> disconnected() {
            return new ConnectionAwareResult<>(null, false);
        }

        public static <T> ConnectionAwareResult<T> resulting(T result) {
            return new ConnectionAwareResult<>(result, true);
        }
    }

    private final Project currentProject;
    private MongoClient client;
    private MongoDBConfigurationResolver configurationResolver;
    private DbElement databaseConnectionPoint;


    public MongoDBFacade(Project project) {
        this.currentProject = project;
    }

    public ConnectionAwareResult<List<MQLIndex>> indexesOfCollection(MongoDBNamespace namespace) {
        if (assertOfflineMode()) {
            return ConnectionAwareResult.disconnected();
        }

        var coll = this.client.getDatabase(namespace.database()).getCollection(namespace.collection());
        var shardingColl = this.client.getDatabase("config").getCollection("collections");
        var shardInfo = shardingColl.find(Filters.eq("_id", "%s.%s".formatted(namespace.database(), namespace.collection()))).limit(1).into(new ArrayList<>(1));
        var indexList = coll.listIndexes();

        if (shardInfo.isEmpty()) {
            var result = indexList.map(MQLIndex::parseIndex).into(new ArrayList<>());
            return ConnectionAwareResult.resulting(result);
        } else {
            var shardKey = MQLIndex.parseIndex(shardInfo.get(0));
            var result = indexList.map(MQLIndex::parseIndex).map(index -> index.isSameAs(shardKey) ? index.markAsShardingKey() : index).into(new ArrayList<>());
            return ConnectionAwareResult.resulting(result);
        }
    }

    public ConnectionAwareResult<Boolean> isCollectionSharded(MongoDBNamespace namespace) {
        if (assertOfflineMode()) {
            return ConnectionAwareResult.disconnected();
        }

        var shardingColl = this.client.getDatabase("config").getCollection("collections");
        var shardInfo = shardingColl.find(Filters.eq("_id", "%s.%s".formatted(namespace.database(), namespace.collection()))).limit(1).into(new ArrayList<>(1));

        return ConnectionAwareResult.resulting(!shardInfo.isEmpty());
    }


    public ConnectionAwareResult<CollectionSchema> schemaOf(MongoDBNamespace namespace) {
        if (assertOfflineMode()) {
            return ConnectionAwareResult.disconnected();
        }

        var indexes = indexesOfCollection(namespace).result();
        var indexedFields = new HashSet<String>();

        for (var idx : indexes) {
            indexedFields.addAll(idx.definition().stream().map(MQLIndex.MQLIndexField::fieldName).toList());
        }

        var coll = this.client.getDatabase(namespace.database()).getCollection(namespace.collection());
        var resultDocs = coll.aggregate(Collections.singletonList(Aggregates.sample(500))).batchSize(500).into(new ArrayList<>());
        var schema = new CollectionSchema(namespace.database(), namespace.collection(), new HashMap<>());
        for (var doc : resultDocs) {
            schema = schema.merge(indexedFields, doc);
        }

        return ConnectionAwareResult.resulting(schema);
    }

    public void openDatabaseEditorAppendingCode(String code) {
        if (assertOfflineMode()) {
            return;
        }

        var activeEditor = EditorTracker.getInstance(currentProject).getActiveEditors().stream().filter(it -> {
            var currentFile = PsiDocumentManager.getInstance(currentProject).getPsiFile(it.getDocument()).getVirtualFile();
            var dataSourceOfFile = DbVFSUtils.getDataSource(currentProject, currentFile);
            return configuration.dataSource().equals(dataSourceOfFile);
        }).findFirst();

        if (activeEditor.isPresent()) {
            var editor = activeEditor.get();
            var currentFile = PsiDocumentManager.getInstance(currentProject).getPsiFile(editor.getDocument()).getVirtualFile();
            FileEditorManager.getInstance(currentProject).openFile(currentFile, true);
        } else {
            var vFile = DbUIUtil.openInConsole(currentProject, configuration.dataSource(), null, "", true);
            var psiFile = PsiManager.getInstance(currentProject).findFile(vFile);
            var document = PsiDocumentManager.getInstance(currentProject).getDocument(psiFile);
            activeEditor = Arrays.stream(EditorFactory.getInstance().getEditors(document, currentProject)).findFirst();
        }

        if (activeEditor.isEmpty()) {
            return;
        }

        var editor = activeEditor.get();
        var document = editor.getDocument();
        var textLength = document.getTextLength();
        if (textLength > 0 && document.getCharsSequence().charAt(textLength - 1) != '\n') {
            WriteCommandAction.runWriteCommandAction(editor.getProject(), null, null, () -> { document.insertString(textLength, "\n"); });
        }

        editor.getCaretModel().moveToOffset(document.getTextLength());
        WriteCommandAction.runWriteCommandAction(editor.getProject(), null, null, () -> {
            document.insertString(document.getTextLength(), code + "\n");
        });

    }

    private boolean assertOfflineMode() {
        if (this.configurationResolver == null) {
            this.configurationResolver = currentProject.getService(MongoDBConfigurationResolver.class);
        }

        if (this.client == null) {
            this.configuration = this.configurationResolver.getMongoDBConnectionConfiguration();
            if (!configuration.isConfigured()) {
                return true;
            }

            this.client = MongoClients.create(new ConnectionString(configuration.url()));
        }

        return false;
    }
}
