<!-- 界面组件。 -->
<template>
  <div class="container content-field">
    <div class="card content-card">
      <div class="card-body">
        <slot></slot>
      </div>
    </div>
  </div>
</template>

<style scoped>
div.content-field {
  margin-top: 22px;
  animation: card-in 320ms ease;
}

.content-card {
  position: relative;
  overflow: hidden;
}

.content-card::before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background: linear-gradient(130deg, rgba(90, 209, 255, 0.11), transparent 34%, rgba(255, 191, 71, 0.08));
}

@keyframes card-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>
