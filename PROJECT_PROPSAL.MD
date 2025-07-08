# NoteQuest: Personal Knowledge-Base Search API

## Purpose & Value  
Build a backend service that lets users drop in a folder of notes (Markdown, plain text, maybe PDFs later) and instantly get powerful, full-text search with tag and metadata filtering. Helps students, writers, or researchers retrieve relevant snippets without wading through file explorers or crude desktop searches.

---

## Key Objectives  
- **Indexing & Watching**: Monitor a filesystem tree for add/modify/delete events and keep your search index in sync.  
- **Search & Ranking**: Provide low-latency query endpoints that return ranked results, snippets showing matched terms, and support boolean/tag filters.  
- **Lightweight Footprint**: Everything must run comfortably on a modest Linux box (4–8 GB RAM), so choose an embedded or file-based index (e.g. SQLite FTS5 or Whoosh) rather than a heavy external search cluster.

---

## Core Challenges to Tackle  
1. **Incremental Indexing**  
   - Detect and process only changed documents without re-indexing the entire corpus.  
2. **Query Performance**  
   - Balance index complexity (stems, n-grams, fuzzy matching) against RAM/CPU limits to keep latency under ~200 ms.  
3. **Consistency & Durability**  
   - Recover gracefully from crashes mid-index without needing a full re-index.

---

## Suggested Functional Components  
- **File Watcher**  
  - A daemon/service using inotify (or similar) that enqueues files for (re)indexing on create/update/delete.  
- **Indexer**  
  - Parses text/Markdown, extracts metadata (YAML front-matter, tags), and writes into your chosen FTS store.  
- **Search API**  
  - REST (or GraphQL) layer that accepts queries, builds filter clauses, hits the index, and returns paginated, snippet-rich results.  
- **Config & State Store**  
  - A small relational store (SQLite) or JSON/YAML file to track document IDs, index version, and user preferences.

---

## Non-Functional Considerations  
- **Startup Time**: How quickly does the API become available after a cold start?  
- **Memory Usage**: What caching strategy prevents reloading the entire index into RAM?  
- **Scalability Paths**: If notes grow into tens of thousands of files, how will you shard or archive old content?

---

## Success Criteria & Demo Ideas  
- **Instant Indexing**: Dropping a new Markdown file into `~/notes/` makes it immediately searchable via `/search?q=…`.  
- **Tag Filters**: Endpoints like `/search?q=recipe&tag=italian` return only Italian-tagged notes.  
- **Performance**: Search latency stays under 200 ms with ~5 000 small text files.  

_Start with a minimal prototype—watch a folder, index all `.md` into FTS5, expose one `/search` endpoint—then iterate on incremental updates, caching strategies, and advanced ranking features._  
