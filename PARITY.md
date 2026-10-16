# Parity Checklist

Every design or behavior detail that cannot be known without verifying against the original iOS app is tracked here.
When implementing defaults, flag with `// VERIFY:` comments in code.

| Area | Feature / Element | Proposed Default | Verification Status | Notes |
| --- | --- | --- | --- | --- |
| Navigation | Screen Model | Horizontal pager (Record, Receipt, Insight) with 3-dot indicator | Proposed | Check against original iOS navigation |
| Splash | Animation & Timing | System splash with 600-800ms fade/scale wordmark | Proposed | Original has custom animated splash clip |
| Theme | Colors | Light: #FFFFFF bg, #F6F6F4 surface, #FBFAF6 paper. Dark: #000000 bg, #121212 surface, #1A1A1A paper | Proposed | Extract with color picker from iOS screenshots |
| Typography | Fonts & Sizes | Inter / system sans-serif, tabular numbers (`tnum`), IBM Plex Mono / JetBrains Mono for receipt | Proposed | Verify against iOS typography |
| Components | Corner Radii | Sheets: 28dp, Cards: 16dp, Chips: fully rounded | Proposed | Measure in screenshots |
| Motion | Animation Curves | Spring damping ~0.85, medium stiffness; content fade 150-200ms; list insert 250ms | Proposed | Measure via 60fps frame counts |
| Categories | Built-in Seed List | Food & Drink, Groceries, Transport, Shopping, Home & Bills, Health, Fun, Travel, Other | Proposed | Check original category names and icons |
| Receipt | Print Animation | Rows revealed top-to-bottom over ~500ms | Proposed | Check if original animates receipt opening |
| Multi-item | Multiple entries | Split on newlines, semicolons, Chinese commas, ASCII comma + space | Proposed | Verify whether iOS app supports multi-item entry |
| Currency | Changing currency | Default from locale, affects new entries only | Proposed | Verify how original handles multiple currencies |
| Spacing | Grid & Margins | 4dp grid, 24dp screen horizontal padding | Proposed | Verify padding values in iOS app |
| Receipt | Perforation Teeth | 6dp tooth width with triangular teeth | Proposed | Check tooth size and shape in iOS receipt |
| Record | Today Date Format | "EEEE, MMM d" in user locale | Proposed | Verify today's date format pattern on iOS |
| Record | Sheet Peek Height | 56dp + navigation bar bottom inset | Proposed | Verify peek height of entry sheet on iOS |
| Entry | Text Field Style | Single line, transparent background, headline font | Proposed | Verify input field styling in entry sheet on iOS |
| Top Bar | Settings Icon | Material Symbols rounded Settings icon | Proposed | Verify icon and placement on iOS |
