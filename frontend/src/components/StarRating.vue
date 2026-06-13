<template>
  <a-rate
    :value="modelValue"
    @change="onChange"
    :count="5"
    :allow-half="allowHalf"
    :disabled="disabled"
  />
</template>

<script setup lang="ts">
const props = withDefaults(
  defineProps<{
    modelValue: number
    allowHalf?: boolean
    disabled?: boolean
  }>(),
  { allowHalf: true, disabled: false }
)
const emit = defineEmits<{ (e: 'update:modelValue', v: number): void }>()
function onChange(v: number) {
  emit('update:modelValue', v)
}
</script>
