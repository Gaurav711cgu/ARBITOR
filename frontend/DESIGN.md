# Design System: Arbiter Control Plane
**Project ID:** arbiter-core-sys

## 1. Visual Theme & Atmosphere
The atmosphere is "High-Frequency Trading Cockpit meets Cybernetic Elegance". It feels expensive, utilitarian, and data-dense but hyper-clean. The layout embraces asymmetric arrangements, avoiding predictable centered heroes in favor of technical dashboards and split-pane telemetry. The aesthetic uses a "Dark Mode First" approach, leveraging Absolute Void black backgrounds offset by microscopic translucent borders ("Liquid Glass") to define structure without heavy boxes.

## 2. Color Palette & Roles
* **Absolute Void (Primary Background):** Deep, pure space black (`#050810`). Used for the main body and all foundational surfaces. Creates infinite depth.
* **Surface Obsidian (Elevated Background):** A very subtle, off-black navy (`#0a0f1d`). Used for floating cards and secondary layers.
* **Electric Amber (Primary Accent/Warning):** A crisp, luminous gold (`#F5A623`). Used for active states, primary call-to-actions, and active ledger metrics.
* **Cybernetic Emerald (Success/Live State):** A bright, sterile green (`#10b981`). Used for successful transactions, connected states, and positive telemetry.
* **Terminal Cyan (Secondary Accent):** A technical blue (`#22d3ee`). Used for latency metrics and secondary interactive elements.
* **Faint Stardust (Muted Text):** A soft, low-contrast gray (`#6b7280`). Used for secondary labels, table headers, and metadata to reduce visual noise.

## 3. Typography Rules
* **Headers & Display:** `Space Grotesk`. Used exclusively for titles, section headers, and brand marks. Tracks tightly (`tracking-tighter`) for a dense, engineered feel.
* **Data & Inputs (Strict Monospace):** `JetBrains Mono`. Used for ALL numbers, tabular data, labels, buttons, and inputs.
* **Variant Enforcements:** All numeric displays MUST use `tabular-nums` (`font-variant-numeric: tabular-nums`) to prevent layout jitter during real-time data streaming.

## 4. Component Stylings
* **AppShell / Navigation:** Floating pill-shaped bar (`rounded-full`) that sits inside the layout. Uses "Liquid Glass" (frosted glass `backdrop-filter: blur(12px)`) with a 1px translucent inner border (`rgba(255,255,255,0.05)`). Active tabs feature an inner glowing pill.
* **Cards / Bento Containers:** Utilitarian geometry with generously rounded corners (`rounded-[2rem]`). Background is translucent `rgba(255, 255, 255, 0.02)` with a 1px `border-white/5` stroke. On hover, these elevate via `-translate-y-1` and cast a subtle, wide diffusion shadow.
* **Data Tables:** Borderless rows (`border-b border-white/5`). Headers are tiny (`text-xs`), uppercase, and use the Faint Stardust color. Rows light up subtly on hover.
* **Buttons / Inputs:** Pill-shaped or heavily rounded rectangles. Inputs feature zero external borders until focused, at which point they glow with a 2px Electric Amber ring.

## 5. Layout Principles
* **Structure:** Contained within a strict `max-w-7xl` wrapper. Uses CSS Grid extensively for asymmetric Bento layouts (e.g., 3-column top row, 2-column bottom row).
* **Perpetual Motion:** The dashboard feels alive. Features pulsing status dots, infinite scrolling data streams, and staggered load-in animations using Spring physics (`stiffness: 100, damping: 20`).
* **Visual Density:** "Cockpit Mode". Elements are packed intelligently using 1px separator lines rather than heavy padding. White space is used surgically to separate major functional zones.
