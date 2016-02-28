export var MoreMessage = React.createClass({
  render: function() {
    return <div>Hello {this.props.name}</div>;
  }
});

ReactDOM.render(<MoreMessage name="John" />, mountNode);
