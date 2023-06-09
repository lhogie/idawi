import ResizablePanels from './ResizablePanels'

export default function  TestResizable({}) {

    
    return (
        <div>
          <h1>ReactJS Resizable Panels</h1>
          <ResizablePanels>
            <div>
              This is the first panel. It will use the rest of the available space.
            </div>
            <div>
              This is the second panel. Starts with 300px.
            </div>
            <div>
              This is the third panel. Starts with 300px.
            </div>
          </ResizablePanels>
        </div>
    )
}