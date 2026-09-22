# 规范

## JS 规范

### 操作符

- 可选链 `?.`

```js
// bad
person && person.name;
// good
person?.name;
person?.likes?.[0];
```

- 空值合并操作符 `??`

```js
// 只有当左侧为 null 和 undefined 时，才会返回右侧的值，而 `||` 逻辑或操作符会在左侧操作数为假值时返回右侧的值。
// if val is not null or undefined
const a = undefined || undefined;
const b = a ?? '';
```

- `||`

```js
// bad
const has = item === 409007 || item === 403005;
// good
const has = [409007, 403005].includes(item);
```

### import

```js
// bad
import xx from '../../../xx/xx';
// good
import xx from '@/xx';
import yy from './yy';
import yy from '../yy';

// 超出一个层级就不要逐级往上找
```

### 对象

```js
// 多使用解构赋值
const { xx } = yy;

// 减少取值次数
// bad
const name = person[id].name;
const age = person[id].age;
// good
const targetPerson = person[id];
const name = targetPerson.name;
const age = targetPerson.age;
```

禁止直接使用 `Vue.prototype.xx` 获取属性值

### 数组

```js
// 多使用解构赋值
// bad
const arr = oldArr.concat(newArr);
// good
const arr = [...oldArr, ...newArr];
```

### 函数

```js

```

### 删除对象属性

```js
const myObj = { foo: 'bar' };
// bad
delete myObj.foo;
// good
Reflect.deleteProperty(myObj, 'foo');
```

### 尽量使用switch

```js
// bad
if(x){
  // do something
} else if(y){
  // do something
} else ...

// god
switch(x){
  case 1:
    // do something
    break
  case 2:
    // do something
    break
}

// god
const obj = {
  a: 1,
  b: 2,
  c: 3
}
return obj[x]

```

## CSS 规范

- 图片引入

```css
/* bad */
background: url('../../../assets/images/xx.png');

/* good */
background: url('~@/assets/images/xx.png');
```

- 减少行内样式的使用

## HTML 规范

- 减少不必要的嵌套

```html
<!-- bad -->
<div class="comp">
  <component />
</div>
<!-- good -->
<component class="comp" />
```

## Vue 文件命名规范

禁止以 `newxxx` 或者 `xxxnew` 的方式命名组件

.vue 文件引入需以首字母大写并驼峰命名，并且在 template 中也是这样

## git 规范

- 每次提交前必须拉代码

## 文件命名规范

- 图片命名

`小写字母_`

- css 类名

`小写字母-小写字母`

## 减少 BUG

1. 对某个属性的值进行操作前必须对属性值类型以及是否存在进行判断
2. 声明变量时必须给定默认值

## 注释

### 文件注释

```vue
<!--
 * @FileDescription: 该文件的描述信息
 * @Author: 作者信息
 * @Date: 文件创建时间
 * @LastEditors: 最后更新作者
 * @LastEditTime: 最后更新时间
 -->
```

### 方法注释

```js
/**
 * @description: 方法描述
 * @param {参数类型} 参数名称
 * @param {参数类型} 参数名称
 * @return 没有返回信息写 void / 有返回信息 {返回类型} 描述信息
 */
```
