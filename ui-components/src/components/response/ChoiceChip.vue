<script setup lang="ts">
defineProps({
  value: {
    type: Number,
    required: true
  },
  color: {
    type: String,
    default: "primary",
  }
})
</script>

<template>
  <v-chip size="x-large"
          variant="elevated"
          class="mr-2"
          :color="color">
    {{ value }}
  </v-chip>
</template>

<style scoped>

</style>
