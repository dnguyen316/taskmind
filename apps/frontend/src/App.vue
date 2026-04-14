<template>
  <main class="page">
    <section class="hero">
      <h1>TaskMind</h1>
      <p>AI-centered Task Manager</p>
      <ul>
        <li>✅ Monorepo initialized</li>
        <li>✅ Vue frontend ready</li>
        <li>✅ Spring Boot backend ready</li>
      </ul>
    </section>
  </main>
</template>

<style scoped>
.page {
  min-height: 100vh;
  display: grid;
  place-items: center;
  background: linear-gradient(180deg, #f8fafc, #eef2ff);
  font-family: Inter, system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
}

.hero {
  width: min(680px, 92vw);
  background: #fff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  padding: 2rem;
  box-shadow: 0 8px 28px rgba(15, 23, 42, 0.08);
}

h1 {
  margin: 0;
  font-size: 2rem;
  color: #0f172a;
}

p {
  margin-top: 0.5rem;
  color: #334155;
}

ul {
  margin-top: 1.25rem;
  padding-left: 1.15rem;
  color: #1e293b;
}

li + li {
  margin-top: 0.5rem;
}
</style>
