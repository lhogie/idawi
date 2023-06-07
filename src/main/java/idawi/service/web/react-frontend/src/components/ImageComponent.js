
export default function ImageComponent({data, height, width}){

    const dataURL = `data:image/png;base64,${data}`;


    return <img src={dataURL} width={width} height={height} />
}