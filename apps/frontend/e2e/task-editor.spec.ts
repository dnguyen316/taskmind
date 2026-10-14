import { expect, test } from '@playwright/test'
import { openEditor } from './support/taskEditor'
const taskId = '11111111-1111-4111-8111-111111111111'

test('slash commands filter, support keyboard selection, and insert real blocks', async ({
  page,
}) => {
  const errors: string[] = []
  page.on('pageerror', (error) => errors.push(error.message))
  await openEditor(page, '')
  const editor = page.getByRole('textbox', { name: 'Task description', exact: true })
  await editor.click()
  await page.keyboard.type('/heading 2')
  await expect(page.getByRole('listbox', { name: 'Add a block' })).toBeVisible()
  await page.keyboard.press('Enter')
  await page.keyboard.type('Release plan')
  await expect(editor.locator('h2')).toHaveText('Release plan')
  await page.keyboard.press('Enter')
  await page.keyboard.type('/todo')
  await page.keyboard.press('Enter')
  await page.keyboard.type('Ship the release')
  await expect(editor.getByRole('checkbox')).toBeVisible()
  await editor.getByRole('checkbox').check()
  await expect(editor.locator('li[data-checked="true"]')).toContainText('Ship the release')
  await page.getByRole('button', { name: 'Save changes', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('All changes saved')
  await page.reload()
  await expect(editor.locator('h2')).toHaveText('Release plan')
  await expect(editor.getByRole('checkbox')).toBeChecked()
  expect(errors).toEqual([])
})

test('formatting, undo/redo and keyboard saving retain content and optimistic version', async ({
  page,
}) => {
  const state = await openEditor(page, 'Release notes')
  const editor = page.getByRole('textbox', { name: 'Task description', exact: true })
  await editor.click()
  await page.keyboard.press('ControlOrMeta+a')
  await page.getByRole('button', { name: 'Bold', exact: true }).click()
  await expect(editor.locator('strong')).toHaveText('Release notes')
  await page.keyboard.press('ControlOrMeta+z')
  await expect(editor.locator('strong')).toHaveCount(0)
  await page.keyboard.press('ControlOrMeta+Shift+z')
  await expect(editor.locator('strong')).toHaveText('Release notes')
  await page.getByLabel('Task title', { exact: true }).fill('Updated launch plan')
  await page.keyboard.press('ControlOrMeta+s')
  await expect(page.getByRole('status')).toHaveText('All changes saved')
  expect(state.patches[0]).toMatchObject({
    version: 1,
    title: 'Updated launch plan',
    description: '**Release notes**',
    priority: 2,
  })
  await expect(page.getByRole('button', { name: 'Save changes', exact: true })).toBeDisabled()
})

test('a failed save keeps the draft and navigation warns before discarding it', async ({
  page,
}) => {
  const state = await openEditor(page)
  state.failSave = true
  await page.getByLabel('Task title', { exact: true }).fill('Keep my draft')
  await page.getByRole('button', { name: 'Save changes', exact: true }).click()
  await expect(page.getByRole('alert')).toBeVisible()
  await expect(page.getByLabel('Task title', { exact: true })).toHaveValue('Keep my draft')
  await expect(page.getByRole('status')).toHaveText('Unsaved changes')
  page.once('dialog', (dialog) => dialog.dismiss())
  await page.getByRole('button', { name: 'Back to tasks' }).click()
  await expect(page).toHaveURL(new RegExp(`/tasks/${taskId}$`))
  state.failSave = false
  await page.getByRole('button', { name: 'Save changes', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('All changes saved')
})

test('block menu duplicates/deletes and slash Escape preserves typed text', async ({ page }) => {
  await openEditor(page, 'Notes')
  const editor = page.getByRole('textbox', { name: 'Task description', exact: true })
  await editor.click()
  await page.getByRole('button', { name: 'Block actions' }).click()
  await page.getByRole('button', { name: 'Duplicate block' }).click()
  await expect(editor.locator('p')).toHaveCount(2)
  await editor.locator('p').last().click()
  await page.getByRole('button', { name: 'Block actions' }).click()
  await page.getByRole('button', { name: 'Delete block' }).click()
  await expect(editor.locator('p')).toHaveCount(1)
  await page.getByRole('button', { name: 'Add block', exact: true }).click()
  await expect(page.getByRole('listbox', { name: 'Add a block' })).toBeVisible()
  await page.keyboard.press('Escape')
  await expect(page.getByRole('listbox', { name: 'Add a block' })).toHaveCount(0)
  await expect(editor).toContainText('/')
})

test('mobile layout stays within the viewport and oversized descriptions cannot save', async ({
  page,
}) => {
  await page.setViewportSize({ width: 390, height: 844 })
  const state = await openEditor(page, '')
  const editor = page.getByRole('textbox', { name: 'Task description', exact: true })
  await editor.fill('x'.repeat(2001))
  await expect(page.getByRole('alert')).toContainText('Shorten it before saving')
  await page.getByRole('button', { name: 'Save changes', exact: true }).click()
  expect(state.patches).toHaveLength(0)
  expect(await page.evaluate(() => document.documentElement.scrollWidth <= window.innerWidth)).toBe(
    true,
  )
})

test('slash arrow navigation, soft line breaks and normal URLs behave predictably', async ({
  page,
}) => {
  await openEditor(page, 'First line\nSecond line')
  const editor = page.getByRole('textbox', { name: 'Task description', exact: true })
  await expect(editor.locator('br')).toHaveCount(1)
  await page.getByRole('button', { name: 'Add block', exact: true }).click()
  await page.keyboard.press('ArrowDown')
  await page.keyboard.press('ArrowDown')
  await page.keyboard.press('Enter')
  await page.keyboard.type('Next steps')
  await expect(editor.locator('h2')).toHaveText('Next steps')
  await page.keyboard.press('Enter')
  await page.keyboard.type('One')
  await page.keyboard.press('Shift+Enter')
  await page.keyboard.type('Two https://example.com/path ')
  await expect(page.getByRole('listbox', { name: 'Add a block' })).toHaveCount(0)
  await expect(editor.locator('p').filter({ hasText: 'One' })).toContainText(
    'Two https://example.com/path',
  )
  await page.getByRole('button', { name: 'Save changes', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('All changes saved')
  await page.reload()
  await expect(editor.locator('h2')).toHaveText('Next steps')
  await expect(editor.locator('br')).toHaveCount(2)
})

test('saving only properties does not normalize or dirty a legacy text description', async ({
  page,
}) => {
  const state = await openEditor(page, 'First line\nSecond line')
  await page.getByLabel('Task title', { exact: true }).fill('A new title')
  await page.getByRole('button', { name: 'Save changes', exact: true }).click()
  await expect(page.getByRole('status')).toHaveText('All changes saved')
  await expect(page.getByRole('button', { name: 'Save changes', exact: true })).toBeDisabled()
  expect(state.patches[0]?.description).toBe('First line\nSecond line')
})
