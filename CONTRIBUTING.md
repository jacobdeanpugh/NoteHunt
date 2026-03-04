# Contributing to NoteHunt

Guidelines for development, testing, and contributing to the NoteHunt project.

## Getting Started

### Prerequisites
- Java 21 or later
- Maven 3.6+
- Git

### Building the Project

```bash
cd notehunt
mvn clean package
```

### Running the Application

```bash
# After building
java -cp target/notehunt-1.0-SNAPSHOT.jar dev.notequest.App
```

The application will:
1. Create `./data` directory for database
2. Initialize H2 database schema
3. Scan configured directory
4. Begin indexing pending files

### Running Tests

```bash
mvn test
```

Currently only 2 test classes exist with minimal coverage.

---

## Project Structure

```
NoteHunt/
├── README.md                 (Quick start)
├── ROADMAP.md               (Development plan)
├── ARCHITECTURE.md          (Technical deep dive)
├── CONTRIBUTING.md          (This file)
├── PROJECT_PROPOSAL.md      (Original requirements)
├── notehunt/
│   ├── pom.xml              (Maven configuration)
│   ├── src/
│   │   ├── main/java/dev/notequest/
│   │   │   ├── App.java
│   │   │   ├── handler/          (Database, events)
│   │   │   ├── service/          (File watch, index, crawl)
│   │   │   ├── events/           (Event classes)
│   │   │   ├── models/           (Database queries)
│   │   │   └── util/             (Config, MD5)
│   │   ├── main/resources/
│   │   │   └── dev/notequest/config.json
│   │   └── test/java/dev/notequest/
│   │       ├── AppTest.java
│   │       └── service/FileWatcherServiceTest.java
│   └── target/              (Build output)
└── data/                    (H2 database - created at runtime)
```

---

## Development Workflow

### 1. Before Starting

- Check [ROADMAP.md](./ROADMAP.md) for priority work items
- Check GitHub Issues to avoid duplicate work
- See [Architecture](./ARCHITECTURE.md) to understand relevant components

### 2. Creating a Feature Branch

```bash
git checkout -b feature/my-feature-name
# or
git checkout -b bugfix/issue-number
```

Branch naming:
- `feature/rest-api` - New feature
- `bugfix/index-corruption` - Bug fix
- `docs/update-readme` - Documentation
- `refactor/database-pool` - Code quality

### 3. Making Changes

**Code Style:**
- Follow existing code patterns
- Use camelCase for variables/methods
- Use UPPER_CASE for constants
- Add comments for complex logic
- Add Javadoc for public methods

**Example:**
```java
/**
 * Indexes a file and adds it to the Lucene index.
 *
 * @param filePath The path to the file to index
 * @throws IOException If file cannot be read
 */
public void indexFile(Path filePath) throws IOException {
    // Implementation...
}
```

### 4. Testing

**Commit Checklist:**
- [ ] Code compiles without errors: `mvn clean compile`
- [ ] Existing tests still pass: `mvn test`
- [ ] New tests added for your changes
- [ ] No compiler warnings
- [ ] No TODO comments without tracking

**Writing Tests:**
```java
public class MyServiceTest {

    private MyService service;

    @Before
    public void setup() {
        service = new MyService();
    }

    @Test
    public void testHappyPath() {
        // Arrange
        String input = "test";

        // Act
        String result = service.process(input);

        // Assert
        assertEquals("expected", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testErrorHandling() {
        service.process(null);  // Should throw
    }
}
```

### 5. Committing Changes

```bash
git add src/
git commit -m "Add REST API search endpoint

- Implement GET /search endpoint
- Support pagination (limit, offset)
- Return results with relevance scores

Closes #42"
```

**Commit Message Format:**
```
<type>(<scope>): <subject>

<body>

<footer>
```

**Type:**
- `feat` - New feature
- `fix` - Bug fix
- `docs` - Documentation
- `refactor` - Code restructuring
- `test` - Test additions
- `perf` - Performance improvement

**Scope:**
- api, database, indexer, watcher, config, etc.

**Subject:**
- Imperative mood ("Add" not "Added")
- No period at end
- Under 50 characters

**Body:**
- Explain what and why, not how
- Wrap at 72 characters
- Separate from subject with blank line

**Footer:**
- Reference issues: `Closes #123`
- Breaking changes: `BREAKING CHANGE: ...`

### 6. Creating a Pull Request

```bash
git push origin feature/my-feature
```

Then create PR on GitHub with:
- Clear description of changes
- Link related issues
- Reference any documentation updates
- Confirm tests pass

---

## Code Review Guidelines

### For Authors
- Keep PRs focused (one feature per PR)
- Add descriptive commit messages
- Include tests for all changes
- Respond to feedback promptly
- Don't force-push after review starts (use new commits)

### For Reviewers
- Check functionality and correctness
- Verify tests cover new code
- Suggest improvements, don't demand
- Approve once satisfied

---

## Common Development Tasks

### Adding a New Feature

1. Create feature branch: `git checkout -b feature/search-api`
2. Edit relevant files
3. Add tests
4. Run `mvn test` and verify all pass
5. Commit with clear message
6. Create PR with description

### Fixing a Bug

1. Create issue with reproduction steps
2. Create branch: `git checkout -b bugfix/issue-123`
3. Write test that reproduces bug
4. Fix the bug
5. Verify test passes
6. Commit and create PR

### Updating Documentation

1. Create branch: `git checkout -b docs/update-readme`
2. Edit .md files
3. Verify formatting (code blocks, links, etc.)
4. Commit and create PR

### Refactoring Code

1. Create branch: `git checkout -b refactor/extract-class`
2. Make structural changes
3. Run `mvn clean test` to ensure no behavior change
4. Commit with description of refactoring
5. Create PR for review

---

## Testing Strategy

### Unit Tests
- Test individual methods in isolation
- Mock external dependencies (database, file system)
- Aim for 70%+ code coverage

**Example:**
```java
@Test
public void testFileFilteringWithTxtExtension() {
    FileWatcherService service = new FileWatcherService();
    assertTrue(service.fileIsInExtensionFilter("notes.txt"));
    assertFalse(service.fileIsInExtensionFilter("notes.pdf"));
}
```

### Integration Tests
- Test components working together
- Use real database (H2 in-memory)
- Test event flow end-to-end

**Example:**
```java
@Test
public void testIndexingCompleteFlow() throws IOException {
    // Setup
    DatabaseHandler db = new DatabaseHandler();
    FileIndexer indexer = new FileIndexer();

    // Create test file
    Path testFile = Files.createTempFile("test", ".txt");
    Files.write(testFile, "search content");

    // Index it
    indexer.indexFile(testFile);

    // Verify in database
    // Verify in Lucene index
}
```

### Running Tests

```bash
# All tests
mvn test

# Single test class
mvn test -Dtest=FileWatcherServiceTest

# Single test method
mvn test -Dtest=FileWatcherServiceTest#testWatchingDirectory

# With coverage report
mvn clean test jacoco:report
# View: target/site/jacoco/index.html
```

---

## Debugging

### Enable Debug Logging

1. Set environment variable:
   ```bash
   export DEBUG=true
   ```

2. Or add to code:
   ```java
   System.setProperty("java.util.logging.level", "FINE");
   ```

### Common Issues

**Issue: H2 Database Lock**
```
org.h2.jdbc.JdbcSQLException: Database is already in use by another process
```

Solution:
- Close other connections: `rm -rf data/`
- Or set AUTO_SERVER=TRUE in connection string (already done)

**Issue: File Not Found in Index**

Solution:
- Verify `directoryPath` in config.json is correct
- Check file matches extension filter (currently `.txt` only)
- Verify file has appropriate permissions

**Issue: Out of Memory**

Solution:
- Increase heap: `java -Xmx2g -cp ...`
- Reduce batch size in config.json
- Check for memory leaks in profiler

### Using a Debugger

IntelliJ IDEA:
1. Set breakpoint (click line number)
2. Run → Debug 'App'
3. Execution pauses at breakpoint
4. Inspect variables in Variables panel

Command-line (Eclipse JPDA):
```bash
mvn -Dmaven.surefire.debug test
# Then attach debugger to localhost:5005
```

---

## Performance Considerations

### Profiling

Use JProfiler, YourKit, or JVM built-ins:

```bash
# Generate flame graph
java -XX:+UnlockDiagnosticVMOptions \
     -XX:+TraceClassLoading \
     -cp target/notehunt-1.0-SNAPSHOT.jar \
     dev.notequest.App

# Memory profiling
java -XX:+PrintGCDetails \
     -Xloggc:gc.log \
     -cp target/notehunt-1.0-SNAPSHOT.jar \
     dev.notequest.App
```

### Optimization Targets

From [ROADMAP.md](./ROADMAP.md#42-performance-optimization):
- Query result caching
- Database indices
- Batch size tuning
- Connection pooling
- Index segment optimization

---

## Documentation

### Writing Documentation

- Use Markdown format (.md files)
- Include code examples
- Add headings for organization
- Link to related files
- Keep lines under 100 characters

### Javadoc Comments

```java
/**
 * Searches the index for documents matching the query.
 *
 * Results are ranked by relevance score (highest first).
 *
 * @param query The search query string
 * @param limit Maximum number of results (default: 10)
 * @return List of SearchResult objects
 * @throws IllegalArgumentException if query is empty
 * @throws IOException if index cannot be read
 */
public List<SearchResult> search(String query, int limit) {
    // ...
}
```

---

## Deployment

### Building a Release

```bash
# Update version in pom.xml
# mvn versions:set -DnewVersion=1.1.0

# Build
mvn clean package

# JAR is at: notehunt/target/notehunt-1.0-SNAPSHOT.jar
```

### Containerizing

Create `Dockerfile`:
```dockerfile
FROM openjdk:21-slim
WORKDIR /app
COPY notehunt/target/notehunt-1.0-SNAPSHOT.jar .
COPY notehunt/src/main/resources ./resources
CMD ["java", "-jar", "notehunt-1.0-SNAPSHOT.jar"]
```

Build:
```bash
docker build -t notehunt:latest .
docker run -v /notes:/notes notehunt:latest
```

---

## Resources

- [Apache Lucene Guide](https://lucene.apache.org/core/latest_release/core/)
- [H2 Database Docs](https://www.h2database.com/)
- [Java NIO Watch Service](https://docs.oracle.com/javase/8/docs/api/java/nio/file/WatchService.html)
- [Guava Library](https://guava.dev/)
- [Maven Documentation](https://maven.apache.org/)

---

## Getting Help

- **Questions:** Open a GitHub Discussion
- **Bugs:** File an Issue with steps to reproduce
- **Security:** Email maintainers privately
- **Architecture:** See [ARCHITECTURE.md](./ARCHITECTURE.md)
- **Roadmap:** See [ROADMAP.md](./ROADMAP.md)

---

## Code of Conduct

- Be respectful and inclusive
- Assume good intentions
- Provide constructive feedback
- Focus on the code, not the person

---

**Last Updated:** 2024-01
