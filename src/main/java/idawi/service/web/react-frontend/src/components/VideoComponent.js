
export default function VideoComponent({data, height, width}){

    const dataURL = `"data:video/mp4;base64,${data}`;
    return (
        <video width={width} height={height} controls>
            <source src={dataURL} type="video/mp4" />
            Your browser does not support the video tag.
        </video>
    )
}