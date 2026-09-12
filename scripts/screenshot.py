import asyncio
from playwright.async_api import async_playwright

async def main():
    async with async_playwright() as p:
        browser = await p.chromium.launch()
        page = await browser.new_page(color_scheme="dark", viewport={"width": 1400, "height": 900})
        # Wait for the server to be up
        for _ in range(10):
            try:
                await page.goto("http://localhost:8080")
                break
            except Exception:
                await asyncio.sleep(1)
        
        # Wait for spline-viewer or a bit of time for rendering
        await asyncio.sleep(2)
        await page.screenshot(path="web/screenshot.png", full_page=True)
        print("Screenshot saved to web/screenshot.png")
        await browser.close()

asyncio.run(main())
