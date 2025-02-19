# elaastic-vue-components

## How to develop a new Vue.js component and integrate it in a Thymeleaf template

### 1. Create a new Vue.js component

### 2. Create a story for the new component

In the `src/stories` folder, create a new file named `YourComponentName.stories.ts` and add the following code:

```typescript
import type {Meta, StoryObj} from '@storybook/vue3'
import YourComponent from '@/path/to/your/component/YourComponent.vue'

const meta: any = {
    title: 'Folder/YourComponent', // The title of the story
    component: YourComponent, // The component to be rendered
    tags: ['autodocs'],
    parameters: {
        docs: {
            description: {
                story: 'This is the description of the story',
            },
        },
    },
} satisfies Meta<typeof YourComponent>
export default meta

type Story = StoryObj<typeof meta>

export const Primary: Story = {
    args: {
        // The props you want to pass to the component
    },
}
```

More on how to set up stories at: https://storybook.js.org/docs/writing-stories

### 3. Create a new version of the bundle with the new component

First find the `main.ts` file in the `src` folder and add your component to the list of components to be exported:

```typescript
import {registerPlugins} from '@/plugins'
import YourComponent from '@/path/to/your/component/YourComponent.vue'


export {
    registerPlugins,
    YourComponent
}
```

Then go to the `build.gradle.kts`, located in `server/build.gradle.kts`, update the version number:

```koltin
//[...]
// You can find it at the top of the file
var uiComponentsVersion = "0.0.20" // Update the version number
//[...]
```

Reload the project and run the following command in the terminal at the root of the project:

```shell
./gradlew updateVueComponents
```

Then you can update the version number of the `application.properties`.

- `server/src/main/resources/application.properties`
- `server/src/test/resources/application.properties`

```properties
# You can find it at the top of the file
ui.components.version=0.0.20 #Update the version number
```

### 4. Integrate the new component in a Thymeleaf template

Create a new template file with following content:

```html
//Copyright

<!DOCTYPE html>
<html lang="en" th:replace="layout/vuejs-app :: vueJsAppLayout(title=~{::title}, content=~{::body}, style=~{})"
      xmlns:th="http://www.thymeleaf.org" xmlns="http://www.w3.org/1999/html">

    [...]

</html>
```

Add a loader:

```html

<body>
<div id="your-component-app" class="ui-app">
    <!-- Loader -->
    <div class="lds-ripple">
        <div></div>
        <div></div>
    </div>
</div>
</body>
```

Add the script that will create the Vue.js app:

```javascript
window.elaastic = {
    initializeYourComponent: (props1, props2) => {
        const app = Vue.createApp(
                ElaasticVueComponents.YourComponent,
                {
                    props1: props1,
                    props2: props2
                    onAnEventTriggerByYourComponent(valueReturned) {
                        window.parent.postMessage({command: 'event-trigger', valueReturned})
                    }
                }
        )
        ElaasticVueComponents.registerPlugins(app)
        app.mount(`#your-component-app`)
    }
}
```

Create a Controller in the backend that will serve the template:

```kotlin
/*
 * Copyright
 */

package org.elaastic.folder

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/ui/folder")
class FolderUIController() {

    @GetMapping("/your-component")
    fun yourComponent() = "folder/components/your-component"
}
```

Use an iframe to get the Thymleaf template of YourComponent

```html
<iframe id="your-component-app" class="transparent-iframe" th:src="@{/ui/folder/your-component}"
            width="100%"></iframe>

    <!--/* Initialise the your-component app & interact with it */-->
    <script th:inline="javascript">
        const iframe = document.getElementById('your-component-app');

        /*[- Trigger the app init function on iframe loading -]*/
        iframe.onload = () => {
            const props1 = 123;
            const props2 = "Lorem";
            iframe.contentWindow.elaastic.initializeYourComponent(props1, props2)
        }
        window.addEventListener('message', (event) => {
            if (event.data.command === 'event-trigger') {
                const valueReturned = event.data.valueReturned;
                console.log(valueReturned)
            }
        })

        const optionIframeResiser = {
            license: 'GPLv3',
            waitForLoad: true
        };
        /*[- Init iframe resize. As we didn't specify one iframe, it will select all -]*/
        iframeResize(optionIframeResiser)
    </script>
```

And that's it! You can now use your new component in a Thymeleaf template.