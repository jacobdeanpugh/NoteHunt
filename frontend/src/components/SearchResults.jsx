import ResultCard from './ResultCard'
import Pagination from './Pagination'

export default function SearchResults({
  results = [],
  totalResults = 0,
  currentPage = 1,
  pageSize = 20,
  loading = false,
  onPageChange,
}) {
  if (loading) {
    return (
      <div className="space-y-4">
        {[...Array(3)].map((_, i) => (
          <div
            key={i}
            className="bg-[#141414] border border-[#1e1e1e] rounded p-6 h-24 animate-pulse"
          />
        ))}
      </div>
    )
  }

  if (totalResults === 0) {
    return (
      <div className="text-center text-[#555555] py-12">
        No results found. Try a different search.
      </div>
    )
  }

  const totalPages = Math.ceil(totalResults / pageSize)

  return (
    <div>
      <h3 className="text-xs uppercase text-[#555555] font-medium tracking-wide mb-4">
        Search Results ({totalResults} found)
      </h3>

      <div className="mb-6">
        {results.map((result, idx) => (
          <ResultCard key={idx} {...result} />
        ))}
      </div>

      <Pagination
        currentPage={currentPage}
        totalPages={totalPages}
        onPageChange={onPageChange}
      />
    </div>
  )
}
