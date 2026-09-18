/** assets/icon-check.svg, assets/icon-add.svg 를 그대로 옮긴 아이콘 —
 * 색은 currentColor 로 바꿔서 쓰이는 곳(버튼 글자색 등)에 맞춰 같이 바뀐다. */
type IconProps = { className?: string }

export function CheckIcon({ className }: IconProps) {
  return (
    <svg
      className={className}
      width="1em"
      height="1em"
      viewBox="0 0 24 24"
      fill="none"
      aria-hidden="true"
    >
      <path
        d="M6 11.6667L10.2162 16L18 8"
        stroke="currentColor"
        strokeWidth="3"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export function AddIcon({ className }: IconProps) {
  return (
    <svg className={className} width="1em" height="1em" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        fillRule="evenodd"
        clipRule="evenodd"
        d="M11.493 3.59201C11.1237 3.73049 10.8216 4.00538 10.649 4.36001L10.54 4.58001L10.52 7.54001L10.5 10.5L7.54004 10.52L4.58004 10.54L4.35904 10.649C4.05541 10.8076 3.80764 11.0554 3.64904 11.359C3.55104 11.557 3.54004 11.624 3.54004 12C3.54004 12.377 3.55104 12.443 3.65004 12.644C3.78204 12.912 4.12504 13.25 4.39004 13.372C4.57904 13.459 4.59804 13.46 7.54004 13.48L10.5 13.5L10.52 16.46C10.54 19.402 10.541 19.421 10.628 19.61C10.75 19.875 11.088 20.218 11.356 20.35C11.557 20.449 11.623 20.46 12 20.46C12.377 20.46 12.443 20.449 12.644 20.35C12.912 20.218 13.25 19.875 13.372 19.61C13.459 19.421 13.46 19.402 13.48 16.46L13.5 13.5L16.46 13.48L19.42 13.46L19.644 13.35C19.912 13.218 20.25 12.875 20.372 12.61C20.499 12.335 20.499 11.665 20.372 11.39C20.25 11.125 19.912 10.782 19.644 10.65L19.42 10.54L16.46 10.52L13.5 10.5L13.48 7.54001C13.46 4.59801 13.459 4.57901 13.372 4.39001C13.25 4.12501 12.912 3.78201 12.647 3.65201C12.352 3.50701 11.809 3.47901 11.493 3.59201Z"
        fill="currentColor"
      />
    </svg>
  )
}
