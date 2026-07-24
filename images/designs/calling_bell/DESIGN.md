---
name: Calling Bell
colors:
  surface: '#121318'
  surface-dim: '#121318'
  surface-bright: '#38393f'
  surface-container-lowest: '#0d0e13'
  surface-container-low: '#1a1b21'
  surface-container: '#1e1f25'
  surface-container-high: '#292a2f'
  surface-container-highest: '#34343a'
  on-surface: '#e3e1e9'
  on-surface-variant: '#b9cacb'
  inverse-surface: '#e3e1e9'
  inverse-on-surface: '#2f3036'
  outline: '#849495'
  outline-variant: '#3b494b'
  surface-tint: '#00dbe9'
  primary: '#dbfcff'
  on-primary: '#00363a'
  primary-container: '#00f0ff'
  on-primary-container: '#006970'
  inverse-primary: '#006970'
  secondary: '#ffb1c4'
  on-secondary: '#65002e'
  secondary-container: '#ff4a8d'
  on-secondary-container: '#590028'
  tertiary: '#fbf3ff'
  on-tertiary: '#42008a'
  tertiary-container: '#e4d1ff'
  on-tertiary-container: '#7d00fb'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#7df4ff'
  primary-fixed-dim: '#00dbe9'
  on-primary-fixed: '#002022'
  on-primary-fixed-variant: '#004f54'
  secondary-fixed: '#ffd9e1'
  secondary-fixed-dim: '#ffb1c4'
  on-secondary-fixed: '#3f001a'
  on-secondary-fixed-variant: '#8f0044'
  tertiary-fixed: '#ecdcff'
  tertiary-fixed-dim: '#d5baff'
  on-tertiary-fixed: '#270057'
  on-tertiary-fixed-variant: '#5e00c1'
  background: '#121318'
  on-background: '#e3e1e9'
  surface-variant: '#34343a'
typography:
  headline-xl:
    fontFamily: Space Grotesk
    fontSize: 48px
    fontWeight: '700'
    lineHeight: 56px
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Space Grotesk
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
  headline-lg-mobile:
    fontFamily: Space Grotesk
    fontSize: 24px
    fontWeight: '700'
    lineHeight: 32px
  body-lg:
    fontFamily: Geist
    fontSize: 18px
    fontWeight: '400'
    lineHeight: 28px
  body-md:
    fontFamily: Geist
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  label-md:
    fontFamily: JetBrains Mono
    fontSize: 14px
    fontWeight: '500'
    lineHeight: 20px
    letterSpacing: 0.05em
  label-sm:
    fontFamily: JetBrains Mono
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.1em
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  unit: 4px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 40px
  container-max: 1280px
  gutter: 20px
---

## Brand & Style
The design system for this product is a fusion of **Glassmorphism** and **Cyberpunk** aesthetics, tailored for a premium, high-tech experience. The brand personality is futuristic, urgent, and highly precise, reflecting the catchphrase: "Instant Chimes. One Tap Away." 

The UI evokes a sense of depth and luminosity through the use of semi-transparent frosted glass layers (alpha 0.3-0.6) and vibrant, glowing gradient borders. The visual narrative centers on "Risk Technologies"—a concept that balances high-stakes performance with sophisticated, data-driven interfaces. The emotional response is one of being "in the cockpit" of a powerful tool, where every interaction is met with a tactile, light-emitting response.

## Colors
The palette is built on high-luminance neons against deep, void-like backgrounds. 
- **Neon Glass (Default):** Utilizes a deep obsidian background with white-tinted glass surfaces.
- **Gradients:** All primary borders use a 45-degree or 90-degree linear gradient transitioning from Cyan (#00F0FF) to Magenta (#FF007F) or Purple (#7F00FF).
- **Functional Colors:** 
  - **Success:** Green to Cyan gradient.
  - **Danger:** Crimson to Orange gradient.
  - **System/Info:** Indigo to Purple.
  - **PRO/Premium:** Gold to Amber.

## Typography
The typography system uses three distinct typefaces to convey a technical, high-performance feel.
- **Space Grotesk** is used for large headlines, providing a geometric, futuristic punch. 
- **Geist** handles the primary body copy, ensuring extreme clarity and a "developer-tool" precision. 
- **JetBrains Mono** is reserved for labels, metadata, and status indicators to emphasize the system’s analytical nature. 

Text should remain crisp white (#FFFFFF) in dark modes with a subtle 0.5px text-shadow on headlines to simulate a "glow" effect. In light mode, use a high-contrast Slate (#0F172A).

## Layout & Spacing
The layout follows a **Fluid Grid** model with a 12-column structure for desktop and a single-column fluid structure for mobile. 
- **Safe Margins:** Use 24px margins on mobile and up to 80px on large desktops.
- **Rhythm:** Elements are spaced using a 4px base unit. Most components use 16px (md) padding to maintain a breathable, airy "glass" feel. 
- **Alignment:** Strictly align all glass containers to the grid. Elements within cards should use consistent internal padding of 20px to allow the gradient borders to "breathe" without crowding the content.

## Elevation & Depth
This design system does not use traditional drop shadows. Depth is achieved through **Backdrop Filters** and **Outer Glows**:
- **Layer 0:** Background (Solid #0A0B10).
- **Layer 1:** Glass Cards (Background Blur: 12px to 20px).
- **Layer 2:** Popovers/Modals (Background Blur: 40px, higher opacity).

**Glow Effects:** Instead of shadows, use `box-shadow: 0 0 15px rgba(0, 240, 255, 0.3)` to create a neon aura around active or primary elements. Borders are 1.5px wide and utilize the linear gradients defined in the color section.

## Shapes
The shape language is "Rounded" to soften the intensity of the neon colors. 
- **Standard Cards:** 0.5rem (8px) corner radius.
- **Interactive Elements:** Buttons and Input fields use a 0.5rem radius to match the card language.
- **Pro Features:** Specifically use "Pill-shaped" (3) roundedness to distinguish "premium" actions or exclusive status chips.

## Components
- **Buttons:** All buttons must have a frosted glass background (`backdrop-filter: blur(8px)`). Primary buttons feature the Cyan-to-Magenta gradient border. On hover, the border glow intensity increases.
- **Chips/Status:** Use JetBrains Mono for text. Backgrounds should be highly transparent (alpha 0.1) with a solid 1px border matching the status color (e.g., Green for success).
- **Lists:** Items are separated by subtle 1px glass dividers (alpha 0.1). Active list items should gain a subtle left-side neon accent bar (3px width).
- **Input Fields:** Semi-transparent dark background. On focus, the border transitions from a low-opacity white to the full Cyan/Magenta gradient.
- **Cards:** The core of the UI. Must have a `backdrop-filter: blur(16px)` and a thin, 1px gradient border.
- **The "Bell" Icon:** A central, glowing component. In the PRO version, it should feature an animated Gold-to-Amber outer glow.