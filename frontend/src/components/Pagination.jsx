export default function Pagination({
  currentPage = 1,
  totalPages = 1,
  onPageChange,
}) {
  const handlePrev = () => {
    if (currentPage > 1) {
      onPageChange(currentPage - 1)
    }
  }

  const handleNext = () => {
    if (currentPage < totalPages) {
      onPageChange(currentPage + 1)
    }
  }

  return (
    <div className="flex justify-center items-center gap-4 text-sm text-[#555555]">
      <button
        onClick={handlePrev}
        disabled={currentPage === 1}
        aria-label="Go to previous page"
        className="hover:text-[#aaaaaa] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        ← Prev
      </button>

      <span aria-live="polite">
        Page {currentPage} of {totalPages}
      </span>

      <button
        onClick={handleNext}
        disabled={currentPage === totalPages}
        aria-label="Go to next page"
        className="hover:text-[#aaaaaa] disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
      >
        Next →
      </button>
    </div>
  )
}
