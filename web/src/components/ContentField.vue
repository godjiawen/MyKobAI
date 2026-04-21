<template>
  <section class="kob-page-shell content-field">
    <div class="kob-surface content-surface">
      <slot></slot>
    </div>
  </section>
</template>

<style scoped>
.content-surface {
  padding: 24px;
}

@media (max-width: 991px) {
  .content-surface {
    padding: 18px;
    border-radius: 24px;
  }
}
</style>
